package com.mentesvivas.backend.service.address;

import java.util.Optional;

public interface CepLookup {

    Optional<CepAddress> find(String cep);
}
