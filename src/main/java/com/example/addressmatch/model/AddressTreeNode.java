package com.example.addressmatch.model;

import com.example.addressmatch.entity.TableA;
import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class AddressTreeNode {
    private String name;
    private Map<String, AddressTreeNode> children = new ConcurrentHashMap<>();
    private List<TableA> addresses = new CopyOnWriteArrayList<>();
    private double weight = 1.0;
    private boolean isLeaf = false;
    private int depth = 0;

    public AddressTreeNode(String name) {
        this.name = name;
    }

    public AddressTreeNode getOrCreateChild(String childName) {
        return children.computeIfAbsent(childName, k -> new AddressTreeNode(k));
    }

    public void addAddress(TableA address) {
        addresses.add(address);
    }

    public boolean hasAddresses() {
        return !addresses.isEmpty();
    }
}