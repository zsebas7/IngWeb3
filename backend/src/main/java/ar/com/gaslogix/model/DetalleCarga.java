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
 * Histórico de lecturas del caudalímetro (tabla detalle_carga). Una orden tiene N detalles; no se guardan
 * todas las lecturas, solo una cada carga.detalle.frecuencia-segundos.
 */
@Entity
@Table(name = "detalle_carga")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DetalleCarga {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Muchos detalles pertenecen a una orden
	@ManyToOne
	@JoinColumn(name = "id_orden", nullable = false)
	private Orden orden;

	// Kg
	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal masaAcumulada;

	@Column(precision = 6, scale = 4, nullable = false)
	private BigDecimal densidad;

	// °C
	@Column(precision = 5, scale = 2, nullable = false)
	private BigDecimal temperatura;

	// Kg/h
	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal caudal;

	// Momento en que el servidor recibió la lectura
	@Column(nullable = false)
	private LocalDateTime fecha;
}
