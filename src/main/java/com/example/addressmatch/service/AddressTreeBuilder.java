package com.example.addressmatch.service;

import com.example.addressmatch.entity.TableA;
import com.example.addressmatch.model.AddressTreeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AddressTreeBuilder {

    @Autowired
    private AddressParserService addressParser;

    private AddressTreeNode root = new AddressTreeNode("中国");

    public void buildAddressTree(List<TableA> addresses) {
        log.info("开始构建地址树，共{}条地址", addresses.size());

        for (TableA address : addresses) {
            insertAddressIntoTree(address);
        }

        calculateNodeDepths(root, 0);
        calculateNodeWeights(root);

        log.info("地址树构建完成");
    }

    private void insertAddressIntoTree(TableA address) {
        List<String> components = addressParser.parseAddressComponents(address.getAddressA());

        AddressTreeNode currentNode = root;

        for (int i = 0; i < components.size(); i++) {
            String component = components.get(i);
            if (component.isEmpty()) {
                component = "未知_" + (i + 1);
            }

            currentNode = currentNode.getOrCreateChild(component);

            if (i == components.size() - 1) {
                currentNode.addAddress(address);
                currentNode.setLeaf(true);
            }
        }
    }

    private void calculateNodeDepths(AddressTreeNode node, int currentDepth) {
        node.setDepth(currentDepth);

        for (AddressTreeNode child : node.getChildren().values()) {
            calculateNodeDepths(child, currentDepth + 1);
        }
    }

    private void calculateNodeWeights(AddressTreeNode node) {
        node.setWeight(Math.log(node.getDepth() + 1));

        for (AddressTreeNode child : node.getChildren().values()) {
            calculateNodeWeights(child);
        }
    }

    public AddressTreeNode getRoot() {
        return root;
    }

    public void clearTree() {
        root = new AddressTreeNode("中国");
    }
}