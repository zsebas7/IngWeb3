package ar.com.gaslogix.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.gaslogix.business.IOrdenBusiness;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.FoundException;
import ar.com.gaslogix.model.Orden;
import ar.com.gaslogix.util.IStandartResponseBusiness;
import ar.com.gaslogix.util.StandartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
			  "fechaCargaPrevista": "2026-11-05T08:00:00",
			  "preset": 10000
			}""";

	@Operation(operationId = "recibir-orden", summary = "Punto 1: recibe una orden de SAP y la deja en estado 1 (pendiente de pesaje inicial).",
			requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
					examples = @ExampleObject(name = "Orden de SAP", value = EJEMPLO_ORDEN))))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Orden creada. La URL de la orden va en el header Location."),
			@ApiResponse(responseCode = "400", description = "JSON inválido o faltan datos obligatorios.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Falta el token o es inválido.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "403", description = "El usuario no tiene rol OPERADOR ni ADMIN.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "409", description = "Ya existe una orden con ese número o código externo.", content = {
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
}
