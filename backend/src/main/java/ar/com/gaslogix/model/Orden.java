package ar.com.gaslogix.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Orden de carga (tabla orden): concentra los datos de todos los eventos del ciclo de carga.
 * Los endpoints identifican la orden por numeroOrden (el que manda SAP), no por el id interno.
 */
@Entity
@Table(name = "orden")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Orden {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Número de orden de SAP: obligatorio y único
	@Column(nullable = false, unique = true)
	private Long numeroOrden;

	// Código opcional del cliente, distinto del número de orden
	@Column(length = 50, unique = true)
	private String codigoExterno;

	// Estado 1 a 4 (ver Constants.ESTADO_...). En la BD es TINYINT
	@Column(nullable = false, columnDefinition = "TINYINT")
	private int estado;

	@ManyToOne
	@JoinColumn(name = "id_camion", nullable = false)
	private Camion camion;

	@ManyToOne
	@JoinColumn(name = "id_chofer", nullable = false)
	private Chofer chofer;

	@ManyToOne
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	// Punto 1: datos que manda SAP y momento en que llegó la orden
	@Column(nullable = false)
	private LocalDateTime fechaCargaPrevista;

	// Kg a cargar
	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal preset;

	@Column(nullable = false)
	private LocalDateTime fechaRecepcion;

	// Punto 2: pesaje inicial (kg) y contraseña de activación
	@Column(precision = 10, scale = 2)
	private BigDecimal tara;

	private LocalDateTime fechaPesajeInicial;

	private Integer passwordActivacion;

	// Punto 3: últimos valores recibidos del caudalímetro (se sobrescriben con cada lectura válida)
	@Column(precision = 10, scale = 2)
	private BigDecimal ultimaMasaAcumulada;

	@Column(precision = 6, scale = 4)
	private BigDecimal ultimaDensidad;

	@Column(precision = 5, scale = 2)
	private BigDecimal ultimaTemperatura;

	@Column(precision = 10, scale = 2)
	private BigDecimal ultimoCaudal;

	private LocalDateTime fechaUltimoDato;

	private LocalDateTime fechaInicioCarga;

	private LocalDateTime fechaFinCarga;

	// Punto 5: pesaje final (kg)
	@Column(precision = 10, scale = 2)
	private BigDecimal pesajeFinal;

	private LocalDateTime fechaPesajeFinal;
}
