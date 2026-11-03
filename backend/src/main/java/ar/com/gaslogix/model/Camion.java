package ar.com.gaslogix.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Camión que llega desde SAP (tabla camion). El total de litros no se guarda: se calcula sumando sus cisternas.
 */
@Entity
@Table(name = "camion")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Camion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Identificador del camión en SAP; puede no venir
	@Column(length = 50, unique = true)
	private String codigoExterno;

	@Column(length = 20, nullable = false, unique = true)
	private String patente;

	@Column(length = 255)
	private String descripcion;

	// La FK está en cisterna (id_camion). CascadeType.ALL: al guardar el camión se guardan también sus cisternas.
	// orphanRemoval: una cisterna que se saca de la lista se borra de la BD (así se reemplazan al sincronizar)
	@OneToMany(mappedBy = "camion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Cisterna> cisternas = new ArrayList<>();
}
