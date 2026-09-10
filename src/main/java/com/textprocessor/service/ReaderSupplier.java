package com.textprocessor.service;

import java.io.IOException;
import java.io.Reader;

@FunctionalInterface
public interface ReaderSupplier {
  Reader open() throws IOException;
}
