package ar.com.gaslogix.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.com.gaslogix.business.ICargaBusiness;
import ar.com.gaslogix.business.exception.BadRequestException;
import ar.com.gaslogix.business.exception.BusinessException;
import ar.com.gaslogix.business.exception.InvalidStateException;
import ar.com.gaslogix.business.exception.NotFoundException;
import ar.com.gaslogix.model.dto.DatoCargaRequest;
import ar.com.gaslogix.model.dto.DatoCargaResponse;
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
@Tag(name = "Carga", description = "Datos en tiempo real del sistema de carga (caudalímetro)")
public class CargaRestController extends BaseRestController {

	@Autowired
	private ICargaBusiness cargaBusiness;

	@Autowired
	private IStandartResponseBusiness response;

	private static final String EJEMPLO_DATO = """
			{
			  "masaAcumulada": 1500.5,
			  "densidad": 0.5432,
			  "temperatura": 18.3,
			  "caudal": 3200
			}""";

	@Operation(operationId = "datos-carga", summary = "Punto 3.b: recibe una lectura del caudalímetro (masa acumulada en kg, densidad, temperatura en °C y caudal en kg/h) para una orden en estado 2.",
			description = "Cada lectura válida sobrescribe los últimos valores de la orden. Además se guarda en el detalle de carga "
					+ "si es la primera o si pasó la frecuencia de almacenamiento desde el último detalle guardado.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json",
					examples = @ExampleObject(name = "Lectura del caudalímetro", value = EJEMPLO_DATO))))
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Lectura aceptada. Indica si además se guardó en el detalle de carga.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = DatoCargaResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "Lectura descartada: falta algún dato, el caudal o la masa acumulada no son mayores a 0, o la masa acumulada es menor a la última recibida.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "401", description = "Falta el token o es inválido.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "403", description = "El usuario no tiene rol INTEGRATION ni ADMIN (por ejemplo, un operador).", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "404", description = "No existe una orden con ese número.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "409", description = "La orden no está en estado 2 (por ejemplo, ya fue cerrada).", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Error interno.", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = StandartResponse.class)) }) })
	// Solo el sistema de carga (usuario técnico con rol INTEGRATION) o un administrador
	@PreAuthorize("hasRole('INTEGRATION') or hasRole('ADMIN')")
	@PostMapping(value = Constants.URL_ORDENES + "/{numeroOrden}/datos-carga",
			consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> recibirDatoCarga(@Parameter(description = "Número de orden de SAP.") @PathVariable Long numeroOrden,
			@RequestBody DatoCargaRequest dato) {
		try {
			return new ResponseEntity<>(cargaBusiness.recibirDatoCarga(numeroOrden, dato), HttpStatus.OK);
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
}
