package ar.com.gaslogix.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
 * Cisterna de un camión (tabla cisterna). Un camión tiene una o varias.
 */
@Entity
@Table(name = "cisterna")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cisterna {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Muchas cisternas pertenecen a un camión. @JsonIgnore evita un bucle infinito al convertir a JSON
	// (camión → cisternas → camión → ...)
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "id_camion", nullable = false)
	private Camion camion;

	@Column(length = 50)
	private String codigoExterno;

	@Column(length = 50, nullable = false)
	private String nombre;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal capacidadLitros;
}
