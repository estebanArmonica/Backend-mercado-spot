package com.safiraenergia.mercadospot.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaResponseDTO;
import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.models.Factura;
import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.repository.IEntidadRepository;
import com.safiraenergia.mercadospot.repository.IFacturaRepository;
import com.safiraenergia.mercadospot.repository.IGlosaRepository;
import com.safiraenergia.mercadospot.repository.IPeriodoRepository;
import com.safiraenergia.mercadospot.services.factura.impl.FacturaServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas del servicio de facturas")
public class TestFacturaService {
    @Mock
    private IFacturaRepository facturaRepository;
    
    @Mock
    private IEntidadRepository entidadRepository;
    
    @Mock
    private IPeriodoRepository periodoRepository;
    
    @Mock
    private IGlosaRepository glosaRepository;
    
    @InjectMocks
    private FacturaServiceImpl facturaService;

    private FacturaDTO facturaDTO;
    private Entidad entidad;
    private Periodo periodo;
    private Glosa glosa;
    private Factura factura;

    /**
     * se les agrega datos a todas las entidades correspondientes
     * @param entidades: se crean datos de pruebas
     * 
    */
    @BeforeEach
    void setUp() {
        entidad = Entidad.builder()
            .id(1L)
            .rutEntidad("76543210-5")
            .nombre("Empresa Test")
            .build();
        
        periodo = Periodo.builder()
            .id(1L)
            .mes(Date.valueOf("2024-01-01"))
            .build();
        
        glosa = Glosa.builder()
            .id(1L)
            .descripcion("TEST")
            .build();
        
        facturaDTO = FacturaDTO.builder()
            .folio(12345L)
            .montoNeto(100000)
            .montoBruto(119000)
            .montoTotal(119000)
            .rutEntidad("76543210-5")
            .nomEntidad("Empresa Test")
            .glosa("TEST")
            .periodo(Date.valueOf("2024-01-01"))
            .estado("PAGADA")
            .build();
        
        factura = Factura.builder()
            .id(1L)
            .folio(12345)
            .montoNeto(100000)
            .entidad(entidad)
            .periodo(periodo)
            .glosa(glosa)
            .build();
    }

    /**
     * Se crea una factura de logica de negocio
     * @param result: el resultado del servicio de factura al llamar a FacturaResponseDTO
    */
    @Test
    @DisplayName("Debe crear una factura exitosamente")
    void shouldCreateFacturaSuccessfully() {
        when(entidadRepository.findByRutEntidad("76543210-5"))
            .thenReturn(Optional.of(entidad));
        when(periodoRepository.findByMes(Date.valueOf("2024-01-01")))
            .thenReturn(Optional.of(periodo));
        when(glosaRepository.findByDescripcion("TEST"))
            .thenReturn(Optional.of(glosa));
        when(facturaRepository.save(any(Factura.class)))
            .thenReturn(factura);
        
        FacturaResponseDTO result = facturaService.createFactura(facturaDTO);
        
        assertThat(result).isNotNull();
        assertThat(result.getFolio()).isEqualTo(12345L);
    }

    /**
     * Este Testing lanzara una Excepción cuando una entidad no existe
    */
    @Test
    @DisplayName("Debe lanzar excepción cuando la entidad no existe")
    void shouldThrowExceptionWhenEntidadNotFound() {
        when(entidadRepository.findByRutEntidad("76543210-5"))
            .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> facturaService.createFactura(facturaDTO))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Entidad not found with RUT");
    }
}
