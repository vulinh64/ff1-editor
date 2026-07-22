package com.ff1.editor.view.shops;

import com.ff1.editor.data.ShopPriceEdit;
import com.ff1.editor.data.ShopPriceSnapshot;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class FxShopPriceRowViewModel {

  private final ShopPriceSnapshot price;
  private final IntegerProperty value;

  public FxShopPriceRowViewModel(ShopPriceSnapshot price) {
    this.price = price;
    this.value = new SimpleIntegerProperty(price.price());
  }

  public String service() {
    return price.serviceColumn() == 0 ? "Bed" : "Service";
  }

  public IntegerProperty valueProperty() {
    return value;
  }

  public void value(int value) {
    this.value.set(value);
  }

  public String source() {
    return "%s @ 0x%08x".formatted(price.sourceEntry(), price.sourceOffset());
  }

  public boolean changed() {
    return value.get() != price.price();
  }

  public ShopPriceEdit toEdit() {
    return new ShopPriceEdit(price.rowIndex(), price.serviceColumn(), value.get());
  }
}
