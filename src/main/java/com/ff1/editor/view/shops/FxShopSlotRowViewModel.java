package com.ff1.editor.view.shops;

import com.ff1.editor.data.ShopGoodSnapshot;
import com.ff1.editor.data.ShopInventoryEdit;
import com.ff1.editor.data.ShopSlotSnapshot;
import java.util.Map;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.lang3.StringUtils;

public final class FxShopSlotRowViewModel {

  private final ShopSlotSnapshot slot;
  private final Map<Integer, ShopGoodSnapshot> goods;
  private final IntegerProperty goodId;

  public FxShopSlotRowViewModel(ShopSlotSnapshot slot, Map<Integer, ShopGoodSnapshot> goods) {
    this.slot = slot;
    this.goods = goods;
    this.goodId = new SimpleIntegerProperty(slot.goodId());
  }

  public int slotNumber() {
    return slot.slotIndex() + 1;
  }

  public IntegerProperty goodIdProperty() {
    return goodId;
  }

  public void goodId(int value) {
    goodId.set(value);
  }

  public String goodName() {
    ShopGoodSnapshot good = goods.get(goodId.get());
    if (good != null) {
      return good.name();
    }
    return goodId.get() == slot.goodId() ? slot.goodName() : "<unknown id>";
  }

  public String category() {
    ShopGoodSnapshot good = goods.get(goodId.get());
    if (good != null) {
      return good.category();
    }
    return goodId.get() == slot.goodId() ? slot.category() : StringUtils.EMPTY;
  }

  public String price() {
    ShopGoodSnapshot good = goods.get(goodId.get());
    Integer price = good == null ? slot.price() : good.price();
    if (price == null) {
      return StringUtils.EMPTY;
    }
    return String.valueOf(price);
  }

  public String source() {
    return "%s @ 0x%08x".formatted(slot.sourceEntry(), slot.sourceOffset());
  }

  public boolean changed() {
    return goodId.get() != slot.goodId();
  }

  public ShopInventoryEdit toEdit() {
    return new ShopInventoryEdit(slot.shopType(), slot.rowIndex(), slot.slotIndex(), goodId.get());
  }
}
