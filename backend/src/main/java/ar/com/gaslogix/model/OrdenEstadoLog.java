package ar.com.gaslogix.model;

import java.time.LocalDateTime;

import ar.com.gaslogix.auth.Usuario;
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
 * Log de auditoría de estados (tabla orden_estado_log): una fila por cada transición de una orden,
 * con quién la hizo y cuándo.
 */
@Entity
@Table(name = "orden_estado_log")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrdenEstadoLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "id_orden", nullable = false)
	private Orden orden;

	// Integer (no int) porque es NULL en la creación de la orden: no hay estado anterior
	@Column(columnDefinition = "TINYINT")
	private Integer estadoAnterior;

	@Column(nullable = false, columnDefinition = "TINYINT")
	private int estadoNuevo;

	@Column(nullable = false)
	private LocalDateTime fecha;

	// Usuario logueado que hizo la transición
	@ManyToOne
	@JoinColumn(name = "id_usuario", nullable = false)
	private Usuario usuario;

	@Column(length = 200)
	private String observaciones;
}
