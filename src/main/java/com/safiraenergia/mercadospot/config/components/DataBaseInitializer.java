package com.safiraenergia.mercadospot.config.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.repository.IEstadoRepository;
import com.safiraenergia.mercadospot.repository.ITipoEntidadRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataBaseInitializer implements CommandLineRunner {
    
    @Autowired
    private IEstadoRepository estadoRepository;
    
    @Autowired
    private ITipoEntidadRepository tipoEntidadRepository;

    @Override
    public void run(String... args) throws Exception {
        // Inicializar estados base
        String[] estadosBase = {"PENDIENTE", "PAGADA", "VENCIDA", "ANULADA", "PARCIAL"};
        for (String estadoName : estadosBase) {
            if (!estadoRepository.existsByDescripcion(estadoName)) {
                estadoRepository.save(Estado.builder().descripcion(estadoName).build());
                log.info("Created estado: {}", estadoName);
            }
        }
        
        // Inicializar tipos de entidad base
        String[] tiposBase = {"DEUDOR", "ACREEDOR"};
        for (String tipo : tiposBase) {
            if (!tipoEntidadRepository.existsByTipoRol(tipo)) {
                tipoEntidadRepository.save(TipoEntidad.builder().tipoRol(tipo).build());
                log.info("Created tipoEntidad: {}", tipo);
            }
        }
        
        log.info("Database initialization completed");
    }
}
