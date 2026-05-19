package com.safiraenergia.mercadospot.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entidad")
public class Entidad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entidad")
    private Long id;

    @Column(name = "rut_entidad", nullable = false, length = 12)
    private String rutEntidad;

    @Column(name = "nomentidad", nullable = false, length = 50)
    private String nombre;

    // relacion de muchos a muchos por table intermedia
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "tipos_entidades", joinColumns = @JoinColumn(name = "entidad_id", referencedColumnName = "id_entidad")
               , inverseJoinColumns = @JoinColumn(name = "tipo_roles_id", referencedColumnName = "id_tipo_entidad") )
    @Builder.Default
    private Set<TipoEntidad> tipoEntidad = new HashSet<>();
}
