package ar.com.gaslogix.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.com.gaslogix.business.IOrdenBusiness;
import ar.com.gaslogix.business.exception.BadPasswordException;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.FoundException;
import ar.com.gaslogix.business.exception.InvalidStateException;
import ar.com.gaslogix.business.exception.NotFoundException;
import ar.com.gaslogix.model.Orden;
import ar.com.gaslogix.model.dto.CierreOrdenResponse;
import ar.com.gaslogix.model.dto.PesajeInicialRequest;
import ar.com.gaslogix.model.dto.PesajeInicialResponse;
import ar.com.gaslogix.model.dto.ValidacionPasswordRequest;
import ar.com.gaslogix.model.dto.ValidacionPasswordResponse;
import ar.com.gaslogix.util.IStandartResponseBusiness;
import ar.com.gaslogix.util.StandartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Órdenes", description = "Ciclo de vida de las órdenes de carga")
public class OrdenRestController extends BaseRestController {

	@Autowired
	private IOrdenBusiness ordenBusiness;

	@Autowired
	private IStandartResponseBusiness response;

	private static final String EJEMPLO_ORDEN = """
			{
			  "numeroOrden": 1001,
			  "codigoExterno": "SAP-ORD-1001",
			  "camion": {
			    "codigoExterno": "SAP-CAM-01",
			    "patente": "AB123CD",
			    "descripcion": "Scania",
			    "cisternas": [
			      { "nombre": "Cisterna 1", "capacidadLitros": 10000 },
			      { "nombre": "Cisterna 2", "capacidadLitros": 5000 }
			    ]
			  },
			  "chofer": { "codigoExterno": "SAP-CHO-01", "nombre": "Juan", "apellido": "Pérez", "documento": "30111222" },
			  "cliente": { "codigoExterno": "SAP-CLI-01", "razonSocial": "Refinor SRL", "contacto": "compras@refinor.com" },
			  "producto": { "codigoExterno": "SAP-PRO-01", "nombre": "Propano", "descripcion": "GLP propano" },
			  "fechaCargaPrevista": "2026-12-15T08:00:00",
			  "preset": 10000
			}""";

	@Operation(operationId = "recibir-orden", summary = "Punto 1: recibe una orden de SAP y la deja en estado 1 (pendiente de pesaje inicial).",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json",
					examples = @ExampleObject(name = "Orden de SAP", value = EJEMPLO_ORDEN))))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Orden creada. La URL de la orden va en el header Location."),
			@ApiResponse(responseCode = "400", description = "JSON inválido o faltan datos obligatorios.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Falta el token o es inválido.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "403", description = "El usuario no tiene rol OPERADOR ni ADMIN.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "409", description = "Ya existe una orden con ese número o código externo, o la patente/documento recibido pertenece a otro camión/chofer.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	@PreAuthorize("hasRole('OPERADOR') or hasRole('ADMIN')")
	@PostMapping(value = Constants.URL_ORDENES, consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> recibirOrden(HttpEntity<String> httpEntity) {
		try {
			// JSON crudo: lo convierte OrdenJsonDeserializer dentro del negocio
			Orden orden = ordenBusiness.recibirOrden(httpEntity.getBody(), getUserLogged());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("Location", Constants.URL_ORDENES + "/" + orden.getNumeroOrden());
			return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
		} catch (BadRequestException e) {
			return new ResponseEntity<>(response.build(HttpStatus.BAD_REQUEST, e, e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (FoundException e) {
			return new ResponseEntity<>(response.build(HttpStatus.CONFLICT, e, e.getMessage()), HttpStatus.CONFLICT);
		} catch (BusinessException e) {
			return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(operationId = "pesaje-inicial", summary = "Punto 2: registra la tara, genera la contraseña de activación y pasa la orden a estado 2.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Pesaje registrado. Devuelve la contraseña de activación.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = PesajeInicialResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "Falta la tara o no es mayor a 0.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Falta el token o es inválido.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "403", description = "El usuario no tiene rol OPERADOR ni ADMIN.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe una orden con ese número.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 1.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	@PreAuthorize("hasRole('OPERADOR') or hasRole('ADMIN')")
	@PostMapping(value = Constants.URL_ORDENES + "/{numeroOrden}/pesaje-inicial",
			consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> registrarPesajeInicial(@Parameter(description = "Número de orden de SAP.") @PathVariable Long numeroOrden,
			@RequestBody PesajeInicialRequest pesaje) {
		try {
			Orden orden = ordenBusiness.registrarPesajeInicial(numeroOrden, pesaje.tara(), getUserLogged());
			return new ResponseEntity<>(new PesajeInicialResponse(orden.getNumeroOrden(), orden.getTara(),
					orden.getFechaPesajeInicial(), orden.getPasswordActivacion()), HttpStatus.OK);
		} catch (BadRequestException e) {
			return new ResponseEntity<>(response.build(HttpStatus.BAD_REQUEST, e, e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NotFoundException e) {
			return new ResponseEntity<>(response.build(HttpStatus.NOT_FOUND, e, e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (InvalidStateException e) {
			return new ResponseEntity<>(response.build(HttpStatus.CONFLICT, e, e.getMessage()), HttpStatus.CONFLICT);
		} catch (BusinessException e) {
			return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(operationId = "validar-password", summary = "Punto 3.a: valida la contraseña de activación y devuelve el preset. Se puede repetir mientras la orden siga en estado 2.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Contraseña correcta. Devuelve el preset.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ValidacionPasswordResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "Falta la contraseña.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Contraseña incorrecta, o falta el token o es inválido.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "403", description = "El usuario no tiene rol OPERADOR ni ADMIN.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe una orden con ese número.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 2.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	@PreAuthorize("hasRole('OPERADOR') or hasRole('ADMIN')")
	@PostMapping(value = Constants.URL_ORDENES + "/{numeroOrden}/validar-password",
			consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> validarPassword(@Parameter(description = "Número de orden de SAP.") @PathVariable Long numeroOrden,
			@RequestBody ValidacionPasswordRequest validacion) {
		try {
			Orden orden = ordenBusiness.validarPasswordActivacion(numeroOrden, validacion.password());
			return new ResponseEntity<>(new ValidacionPasswordResponse(orden.getPreset()), HttpStatus.OK);
		} catch (BadRequestException e) {
			return new ResponseEntity<>(response.build(HttpStatus.BAD_REQUEST, e, e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (BadPasswordException e) {
			return new ResponseEntity<>(response.build(HttpStatus.UNAUTHORIZED, e, e.getMessage()), HttpStatus.UNAUTHORIZED);
		} catch (NotFoundException e) {
			return new ResponseEntity<>(response.build(HttpStatus.NOT_FOUND, e, e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (InvalidStateException e) {
			return new ResponseEntity<>(response.build(HttpStatus.CONFLICT, e, e.getMessage()), HttpStatus.CONFLICT);
		} catch (BusinessException e) {
			return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(operationId = "cerrar-orden", summary = "Punto 4: cierra una orden en carga y la pasa a estado 3. Desde ese momento no acepta más lecturas.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Orden cerrada. Devuelve el estado nuevo y la última masa acumulada.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = CierreOrdenResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Falta el token o es inválido.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "403", description = "El usuario no tiene rol OPERADOR ni ADMIN.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe una orden con ese número.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 2 o todavía no recibió datos de carga.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	@PreAuthorize("hasRole('OPERADOR') or hasRole('ADMIN')")
	@PostMapping(value = Constants.URL_ORDENES + "/{numeroOrden}/cierre", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> cerrarOrden(@Parameter(description = "Número de orden de SAP.") @PathVariable Long numeroOrden) {
		try {
			Orden orden = ordenBusiness.cerrarOrden(numeroOrden, getUserLogged());
			return new ResponseEntity<>(new CierreOrdenResponse(orden.getNumeroOrden(), orden.getEstado(),
					orden.getUltimaMasaAcumulada(), orden.getFechaFinCarga()), HttpStatus.OK);
		} catch (NotFoundException e) {
			return new ResponseEntity<>(response.build(HttpStatus.NOT_FOUND, e, e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (InvalidStateException e) {
			return new ResponseEntity<>(response.build(HttpStatus.CONFLICT, e, e.getMessage()), HttpStatus.CONFLICT);
		} catch (BusinessException e) {
			return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
