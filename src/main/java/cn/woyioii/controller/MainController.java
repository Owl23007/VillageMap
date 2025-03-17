package cn.woyioii.handler;

import cn.woyioii.controller.MainController;
import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class UIEventHandler {
    private final VillageService villageService;
    private final RoadService roadService;
    private final MainController controller;

    public UIEventHandler(VillageService villageService, RoadService roadService, MainController controller) {
        this.villageService = villageService;
        this.roadService = roadService;
        this.controller = controller;
    }

    public void handleAddVillage() {
        // 创建对话框
        Dialog<Village> dialog = new Dialog<>();
        dialog.setTitle("添加村庄");
        dialog.setHeaderText("请输入村庄信息");

        // 设置按钮
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField xField = new TextField();
        TextField yField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("X坐标:"), 0, 2);
        grid.add(xField, 1, 2);
        grid.add(new Label("Y坐标:"), 0, 3);
        grid.add(yField, 1, 3);
        grid.add(new Label("描述:"), 0, 4);
        grid.add(descriptionArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String id = idField.getText();
                    String name = nameField.getText();
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    String description = descriptionArea.getText();
                    Village village = new Village();

                    village.setId(Integer.parseInt(id));
                    village.setName(name);
                    village.setLocateX(x);
                    village.setLocateY(y);
                    village.setDescription(description);

                    return village;
                } catch (NumberFormatException e) {
                    AlertUtils.showError("输入错误", "坐标必须是有效的数字");
                    return null;
                }
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<Village> result = dialog.showAndWait();
        result.ifPresent(village -> {
            if (villageService.addVillage(village)) {
                controller.markDataAsModified();
                controller.refreshUI();
                AlertUtils.showInformation("添加成功", "村庄添加成功");
            } else {
                AlertUtils.showError("添加失败", "可能ID已存在或数据无效");
            }
        });
    }

    public void handleAddRoad() {
        // 创建对话框
        Dialog<Road> dialog = new Dialog<>();
        dialog.setTitle("添加道路");
        dialog.setHeaderText("请输入道路信息");

        // 设置按钮
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 创建村庄下拉选择框
        ComboBox<Village> startVillageCombo = new ComboBox<>();
        ComboBox<Village> endVillageCombo = new ComboBox<>();
        startVillageCombo.getItems().addAll(villageService.getAllVillages());
        endVillageCombo.getItems().addAll(villageService.getAllVillages());

        TextField nameField = new TextField();

        grid.add(new Label("起点村庄:"), 0, 0);
        grid.add(startVillageCombo, 1, 0);
        grid.add(new Label("终点村庄:"), 0, 1);
        grid.add(endVillageCombo, 1, 1);
        grid.add(new Label("道路名称:"), 0, 2);
        grid.add(nameField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    Village start = startVillageCombo.getValue();
                    Village end = endVillageCombo.getValue();
                    String name = nameField.getText();

                    if (start == null || end == null) {
                        AlertUtils.showError("输入错误", "请选择起点和终点村庄");
                        return null;
                    }

                    if (start.equals(end)) {
                        AlertUtils.showError("输入错误", "起点和终点不能是同一个村庄");
                        return null;
                    }

                    // 使用辅助方法计算距离，确保一致性
                    double length = calculateDistance(start, end);

                    return new Road(start.getId(), end.getId(), name, length);
                } catch (Exception e) {
                    AlertUtils.showError("输入错误", "创建道路时出错: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<Road> result = dialog.showAndWait();
        result.ifPresent(road -> {
            if (roadService.addRoad(road)) {
                controller.markDataAsModified();
                controller.refreshUI();
                AlertUtils.showInformation("添加成功", "道路添加成功");
            } else {
                AlertUtils.showError("添加失败", "可能该路径已存在或数据无效");
            }
        });
    }

    public void handleDeleteVillage() {
        Village selected = controller.getSelectedVillage();
        if (selected == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要删除的村庄");
            return;
        }

        // 检查是否有相关联的道路
        boolean hasConnectedRoads = roadService.getAllRoads().stream()
                .anyMatch(road -> road.getStartId().equals(selected.getId()) ||
                        road.getEndId().equals(selected.getId()));

        if (hasConnectedRoads) {
            boolean confirm = AlertUtils.showConfirmation("确认删除",
                    "删除该村庄将同时删除所有相关联的道路，是否继续？");
            if (!confirm) return;
        } else {
            boolean confirm = AlertUtils.showConfirmation("确认删除",
                    "确定要删除村庄 " + selected.getName() + " 吗？");
            if (!confirm) return;
        }

        // 执行删除操作
        if (villageService.deleteVillage(selected.getId())) {
            // 删除相关道路
            if (hasConnectedRoads) {
                roadService.getAllRoads().stream()
                        .filter(road -> road.getStartId().equals(selected.getId()) ||
                                road.getEndId().equals(selected.getId()))
                        .forEach(road -> roadService.deleteRoad(road));
            }

            controller.markDataAsModified();
            controller.refreshUI();
            AlertUtils.showInformation("删除成功", "村庄已成功删除");
        } else {
            AlertUtils.showError("删除失败", "无法删除村庄，请稍后重试");
        }
    }

    public void handleDeleteRoad() {
        Road selected = controller.getSelectedRoad();
        if (selected == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要删除的道路");
            return;
        }

        boolean confirm = AlertUtils.showConfirmation("确认删除",
                "确定要删除这条道路吗？");
        if (!confirm) return;

        if (roadService.deleteRoad(selected)) {
            controller.markDataAsModified();
            controller.refreshUI();
            AlertUtils.showInformation("删除成功", "道路已成功删除");
        } else {
            AlertUtils.showError("删除失败", "无法删除道路，请稍后重试");
        }
    }

    public void handleCalculateShortestPath() {
        // 最短路径计算逻辑保留原有注释
        // 1. 选择起点和终点村庄
        // 2. 调用roadService.calculateShortestPath方法
        // 3. 显示结果
    }

    public void handleEditVillage() {
        // 获取选中的村庄
        Village selectedVillage = controller.getSelectedVillage();
        if (selectedVillage == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要编辑的村庄");
            return;
        }

        // 创建对话框
        Dialog<Village> dialog = new Dialog<>();
        dialog.setTitle("编辑村庄");
        dialog.setHeaderText("修改村庄信息");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // ID不可编辑，显示为标签
        Label idValueLabel = new Label(String.valueOf(selectedVillage.getId()));

        TextField nameField = new TextField(selectedVillage.getName());
        TextField xField = new TextField(String.valueOf(selectedVillage.getLocateX()));
        TextField yField = new TextField(String.valueOf(selectedVillage.getLocateY()));
        TextArea descriptionArea = new TextArea(selectedVillage.getDescription());
        descriptionArea.setPrefRowCount(3);

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(new Label("名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("X坐标:"), 0, 2);
        grid.add(xField, 1, 2);
        grid.add(new Label("Y坐标:"), 0, 3);
        grid.add(yField, 1, 3);
        grid.add(new Label("描述:"), 0, 4);
        grid.add(descriptionArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText();
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    String description = descriptionArea.getText();

                    Village updatedVillage = new Village();
                    updatedVillage.setId(selectedVillage.getId()); // 保持原来的ID
                    updatedVillage.setName(name);
                    updatedVillage.setLocateX(x);
                    updatedVillage.setLocateY(y);
                    updatedVillage.setDescription(description);

                    return updatedVillage;
                } catch (NumberFormatException e) {
                    AlertUtils.showError("输入错误", "坐标必须是有效的数字");
                    return null;
                }
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<Village> result = dialog.showAndWait();
        result.ifPresent(village -> {
            if (villageService.updateVillage(village)) {
                controller.markDataAsModified();
                controller.refreshUI();
                AlertUtils.showInformation("更新成功", "村庄信息已更新");

                // 如果村庄位置发生变化，需要更新相关道路的长度
                if (selectedVillage.getLocateX() != village.getLocateX() ||
                        selectedVillage.getLocateY() != village.getLocateY()) {
                    updateConnectedRoads(village.getId());
                }
            } else {
                AlertUtils.showError("更新失败", "无法更新村庄信息，请检查数据有效性");
            }
        });
    }

    // 更新与指定村庄相连的所有道路长度
    private void updateConnectedRoads(int villageId) {
        roadService.getAllRoads().stream()
                .filter(road -> road.getStartId() == villageId || road.getEndId() == villageId)
                .forEach(road -> {
                    Village start = villageService.getVillageById(road.getStartId());
                    Village end = villageService.getVillageById(road.getEndId());
                    if (start != null && end != null) {
                        double newLength = calculateDistance(start, end);
                        road.setLength(newLength);
                        roadService.updateRoad(road);
                    }
                });
    }

    public void handleEditRoad() {
        // Get selected road
        Road selectedRoad = controller.getSelectedRoad();
        if (selectedRoad == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要编辑的道路");
            return;
        }

        // Create dialog
        Dialog<Road> dialog = new Dialog<>();
        dialog.setTitle("编辑道路");
        dialog.setHeaderText("修改道路信息");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Get current villages
        Village startVillage = villageService.getVillageById(selectedRoad.getStartId());
        Village endVillage = villageService.getVillageById(selectedRoad.getEndId());

        Label idValueLabel = new Label(String.valueOf(selectedRoad.getId()));

        // Create village dropdowns
        ComboBox<Village> startVillageCombo = new ComboBox<>();
        ComboBox<Village> endVillageCombo = new ComboBox<>();
        startVillageCombo.getItems().addAll(villageService.getAllVillages());
        endVillageCombo.getItems().addAll(villageService.getAllVillages());

        // Set default selections
        if (startVillage != null) startVillageCombo.setValue(startVillage);
        if (endVillage != null) endVillageCombo.setValue(endVillage);

        TextField nameField = new TextField(selectedRoad.getName());

        // Make length field read-only
        TextField lengthField = new TextField(String.valueOf(selectedRoad.getLength()));
        lengthField.setEditable(false);
        lengthField.setStyle("-fx-background-color: #f0f0f0;");

        // Update length when villages change
        startVillageCombo.setOnAction(e -> updateLength(startVillageCombo, endVillageCombo, lengthField));
        endVillageCombo.setOnAction(e -> updateLength(startVillageCombo, endVillageCombo, lengthField));

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idValueLabel, 1, 0);
        grid.add(new Label("起点村庄:"), 0, 1);
        grid.add(startVillageCombo, 1, 1);
        grid.add(new Label("终点村庄:"), 0, 2);
        grid.add(endVillageCombo, 1, 2);
        grid.add(new Label("道路名称:"), 0, 3);
        grid.add(nameField, 1, 3);
        grid.add(new Label("长度(km):"), 0, 4);
        grid.add(lengthField, 1, 4);
        grid.add(new Label("(自动根据村庄位置计算)"), 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Village start = startVillageCombo.getValue();
                    Village end = endVillageCombo.getValue();
                    String name = nameField.getText();

                    if (start == null || end == null) {
                        AlertUtils.showError("输入错误", "请选择起点和终点村庄");
                        return null;
                    }

                    if (start.equals(end)) {
                        AlertUtils.showError("输入错误", "起点和终点不能是同一个村庄");
                        return null;
                    }

                    // Calculate length based on village positions
                    double length = calculateDistance(start, end);

                    // Create new road object with original ID
                    Road updatedRoad = new Road(start.getId(), end.getId(), name, length);
                    return updatedRoad;
                } catch (Exception e) {
                    AlertUtils.showError("输入错误", "处理数据时出错: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Road> result = dialog.showAndWait();
        result.ifPresent(road -> {
            if (roadService.updateRoad(road)) {
                controller.markDataAsModified();
                controller.refreshUI();
                AlertUtils.showInformation("更新成功", "道路信息已更新");
            } else {
                AlertUtils.showError("更新失败", "无法更新道路信息，请检查数据有效性");
            }
        });
    }

    // Helper method to calculate distance between villages
    private double calculateDistance(Village start, Village end) {
        return Math.round(Math.sqrt(
                Math.pow(end.getLocateX() - start.getLocateX(), 2) +
                        Math.pow(end.getLocateY() - start.getLocateY(), 2)
        ) * 10) / 10.0;
    }

    // Helper method to update length field
    private void updateLength(ComboBox<Village> startCombo, ComboBox<Village> endCombo, TextField lengthField) {
        Village start = startCombo.getValue();
        Village end = endCombo.getValue();

        if (start != null && end != null) {
            double length = calculateDistance(start, end);
            lengthField.setText(String.valueOf(length));
        }
    }
}