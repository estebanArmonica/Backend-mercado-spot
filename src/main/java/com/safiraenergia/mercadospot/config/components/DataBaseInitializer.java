package com.safiraenergia.mercadospot.config.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.repository.IEstadoRepository;
import com.safiraenergia.mercadospot.repository.ITipoEntidadRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
public class DataBaseInitializer implements CommandLineRunner {
    
    @Autowired
    private IEstadoRepository estadoRepository;
    
    @Autowired
    private ITipoEntidadRepository tipoEntidadRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando inicialización de datos base...");
        
        try {
            // Inicializar estados base
            inicializarEstados();
            
            // Inicializar tipos de entidad base
            inicializarTiposEntidad();
            
            log.info("Inicialización de datos base completada exitosamente");
        } catch (Exception e) {
            log.error("Error durante inicialización de datos base: {}", e.getMessage(), e);
            // No lanzar la excepción para que la aplicación pueda continuar
            // Los datos se crearán bajo demanda durante el ETL si no existen
        }
    }

    private void inicializarEstados() {
        String[] estadosBase = {"PENDIENTE", "PAGADA"};
        
        for (String estadoName : estadosBase) {
            try {
                if (!estadoRepository.existsByDescripcion(estadoName)) {
                    Estado estado = Estado.builder().descripcion(estadoName).build();
                    estadoRepository.save(estado);
                    log.info("Estado creado: {}", estadoName);
                } else {
                    log.debug("Estado ya existe: {}", estadoName);
                }
            } catch (Exception e) {
                log.warn("No se pudo crear el estado {}: {}", estadoName, e.getMessage());
            }
        }
    }

    private void inicializarTiposEntidad() {
        String[] tiposBase = {"DEUDOR", "ACREEDOR"};
        
        for (String tipo : tiposBase) {
            try {
                if (!tipoEntidadRepository.existsByTipoRol(tipo)) {
                    TipoEntidad tipoEntidad = TipoEntidad.builder().tipoRol(tipo).build();
                    tipoEntidadRepository.save(tipoEntidad);
                    log.info("Tipo de entidad creado: {}", tipo);
                } else {
                    log.debug("Tipo de entidad ya existe: {}", tipo);
                }
            } catch (Exception e) {
                log.warn("No se pudo crear el tipo de entidad {}: {}", tipo, e.getMessage());
            }
        }
    }
}
