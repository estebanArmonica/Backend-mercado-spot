package com.safiraenergia.mercadospot.etl.loader;

import java.util.List;

import lombok.Value;

@Value
public class LoadResult {
    int inserted;
    int updated;
    int skipped;
    List<String> errors;
}
