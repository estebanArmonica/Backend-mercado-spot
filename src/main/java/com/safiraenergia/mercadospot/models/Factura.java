package com.safiraenergia.mercadospot.models;

import java.util.Date;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "factura")
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Long id;

    @Column(name = "folio", nullable = false)
    private int folio;
    
    @Column(name = "monto_neto", nullable = false)
    private int montoNeto;
    
    @Column(name = "monto_bruto", nullable = false)
    private int montoBruto;
    
    @Column(name = "monto_total", nullable = false)
    private int montoTotal;
    
    @Column(name = "fecha_emision", nullable = true)
    private Date fechaEmision;
    
    @Column(name = "fecha_pago", nullable = true)
    private Date fechaPago;
    
    // relaciones de uno a muchos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id", nullable = false)
    private Entidad entidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glosa_id", nullable = false)
    private Glosa glosa;

    // relacion muchos a muchos por tabla intermedia
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "estados_facturas",
        joinColumns = @JoinColumn(name = "factura_id", referencedColumnName = "id_factura"),
        inverseJoinColumns = @JoinColumn(name = "estado_id", referencedColumnName = "id_estado"))
    @Builder.Default
    private Set<Estado> estados = new HashSet<>();
}