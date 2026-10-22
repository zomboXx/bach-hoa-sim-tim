package vn.simtim.api.sale.application;

import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import vn.simtim.api.sale.domain.PosCart;
import vn.simtim.api.sale.domain.SaleValidationException;

@Component
public class TransferSettings {
    private final Environment env;
    public TransferSettings(Environment env) { this.env=env; }
    public Config config(PosCart.Scope scope) {
        String bin=env.getProperty("TRANSFER_BANK_BIN", "").trim();
        String number=env.getProperty("TRANSFER_ACCOUNT_NUMBER", "").trim();
        String name=env.getProperty("TRANSFER_ACCOUNT_NAME", "").trim();
        boolean enabled=env.getProperty("TRANSFER_ENABLED",Boolean.class,false)
                && scope.organizationId().toString().equals(env.getProperty("TRANSFER_ORGANIZATION_ID"))
                && scope.storeId().toString().equals(env.getProperty("TRANSFER_STORE_ID"))
                && bin.matches("[0-9]{6}") && number.matches("[A-Za-z0-9]{1,19}")
                && !name.isBlank() && name.length()<=100;
        return new Config(enabled,enabled?bin:null,enabled?number:null,enabled?name:null,10);
    }
    public Config require(PosCart.Scope scope) {
        Config config=config(scope);
        if (!config.enabled()) throw new SaleValidationException("TRANSFER_NOT_CONFIGURED", "Chuyển khoản chưa được cấu hình cho cửa hàng này.");
        return config;
    }
    public record Config(boolean enabled,String bankBin,String accountNumber,String accountName,int expiresInMinutes) {}
}
