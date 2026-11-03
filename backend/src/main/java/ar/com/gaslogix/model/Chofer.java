package ar.com.gaslogix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chofer que llega desde SAP (tabla chofer).
 */
@Entity
@Table(name = "chofer")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Chofer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 50, unique = true)
	private String codigoExterno;

	@Column(length = 100, nullable = false)
	private String nombre;

	@Column(length = 100, nullable = false)
	private String apellido;

	// Identificador alternativo cuando no viene el código externo
	@Column(length = 20, unique = true)
	private String documento;
}
