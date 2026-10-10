package vn.simtim.api.inventory.application;

import vn.simtim.api.inventory.domain.StocktakeException;
import vn.simtim.api.inventory.domain.StocktakeLine;

/**
 * Ném khi server phát hiện xung đột version tồn kho.
 * Mang theo dòng đã lưu để HTTP adapter trả body đầy đủ (409).
 */
public class StocktakeConflictException extends StocktakeException {

    private final StocktakeLine savedLine;

    public StocktakeConflictException(StocktakeLine savedLine) {
        super("CONFLICT", savedLine.conflictReason());
        this.savedLine = savedLine;
    }

    public StocktakeLine getSavedLine() { return savedLine; }
}
