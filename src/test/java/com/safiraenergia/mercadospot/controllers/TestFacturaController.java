package com.safiraenergia.mercadospot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safiraenergia.mercadospot.controller.FacturaController;
import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaResponseDTO;
import com.safiraenergia.mercadospot.services.factura.IFacturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;;

@WebMvcTest(FacturaController.class)
@DisplayName("Pruebas del controlador de facturas")
public class TestFacturaController {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private IFacturaService facturaService;
    
    private FacturaDTO facturaDTO;
    private FacturaResponseDTO facturaResponseDTO;

    /**
     * Agregamos datos al dto de factura para realizar las cargas de datos de forma 
     * manual 
    */
    @BeforeEach
    void setUp(){
        facturaDTO = FacturaDTO.builder()
            .folio(12345L)
            .montoNeto(100000)
            .montoBruto(119000)
            .montoTotal(119000)
            .fechaEmision(Date.valueOf("2024-01-15"))
            .fechaPago(Date.valueOf("2024-02-15"))
            .rutEntidad("76543210-5")
            .nomEntidad("Empresa Test")
            .glosa("TEST")
            .periodo(Date.valueOf("2024-01-01"))
            .estado("PAGADA")
            .tipoEntidad("ACREEDOR")
            .build();
        
        facturaResponseDTO = FacturaResponseDTO.builder()
            .id(1L)
            .folio(12345L)
            .montoNeto(100000)
            .montoBruto(119000)
            .montoTotal(119000)
            .rutEntidad("76543210-5")
            .nombreEntidad("Empresa Test")
            .glosa("TEST")
            .build();
    }

    /**
     * En este testing nos devolvera una lista de todas las facturas paginadas y disponibles
     * en nuestra base de datos
     * @param Page<FacturaResponseDTO>: nos devuelve una lista de todas las facturas
     * @throws Exception: en caso de problemas nos devuelve una excepción
    */
    @Test
    @DisplayName("Debe obtener todas las facturas paginadas")
    void shouldGetAllFacturas() throws Exception {
        Page<FacturaResponseDTO> page = new PageImpl<>(List.of(facturaResponseDTO));
        
        when(facturaService.getAllFacturas(any(Pageable.class)))
            .thenReturn(page);
        
        mockMvc.perform(get("/api/v1/facturas")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].folio").value(12345))
                .andExpect(jsonPath("$.content[0].nomEntidad").value("Empresa Test"));
    }

    /**
     * Este Testing nos devuelve una factura por busqueda en id
     * @param ID: nos devuelve una factura por busqueda de su ID 
     * @throws Exception: si no encuentra nada nos devuelve una excepción
    */
    @Test
    @DisplayName("Debe obtener factura por ID")
    void shouldGetFacturaById() throws Exception {
        when(facturaService.getFacturaById(1L))
            .thenReturn(facturaResponseDTO);
        
        mockMvc.perform(get("/api/v1/facturas/1")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.folio").value(12345));
    }

    /**
     * Este Testing nos crea una nueva factura
     * @param ObjectMapper: nos devuelve un objecto mapeado en formato String el cual crea un JSON
     * @throws Exception: en caso de problemas de mapeo lanza excepción
    */
    @Test
    @DisplayName("Debe crear una nueva factura")
    void shouldCreateFactura() throws Exception {
        when(facturaService.createFactura(any(FacturaDTO.class)))
            .thenReturn(facturaResponseDTO);
        
        mockMvc.perform(post("/api/v1/facturas")
                .with(user("admin").roles("ADMIN", "ETL_OPERATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(facturaDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio").value(12345));
    }

    /**
     * Este Testing nos ayuda para eliminar un proceso de factura mal ingresada (pero solo admin)
     * @param rol_admin: si tiene el rol de admin podra realizar la eliminación
     * @throws Exception: nos lanzara una excepcion en caso de problemas de rol
    */
    @Test
    @DisplayName("Debe eliminar una factura (solo ADMIN)")
    void shouldDeleteFactura() throws Exception {
        mockMvc.perform(delete("/api/facturas/1")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }
}
