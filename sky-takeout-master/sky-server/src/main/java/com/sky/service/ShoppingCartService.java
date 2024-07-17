package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    List<ShoppingCart> showShoppingCart();


    void add(ShoppingCartDTO shoppingCartDTO);

    void clean();

    void deleteOne(ShoppingCartDTO shoppingCartDTO);
}
