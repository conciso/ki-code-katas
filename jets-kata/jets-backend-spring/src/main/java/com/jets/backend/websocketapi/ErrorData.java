package com.jets.backend.websocketapi;

import com.jets.backend.core.service.ErrorCode;

record ErrorData(ErrorCode code, String message) {}
