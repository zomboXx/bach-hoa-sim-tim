package vn.simtim.api.catalog.domain;

/**
 * Raw binary data and MIME content type of a product image.
 */
public record ProductImage(byte[] data, String contentType) {}
