package vn.simtim;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Model.Product, String> {
  List<Model.Product> findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
      String name, String barcode);
}
