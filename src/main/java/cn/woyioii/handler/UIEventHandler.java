package cn.woyioii.handler;

import cn.woyioii.model.Road;
import cn.woyioii.model.Village;
import cn.woyioii.service.RoadService;
import cn.woyioii.service.VillageService;
import cn.woyioii.util.AlertUtils;
import cn.woyioii.util.MapCalculator;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class UIEventHandler {
    // 事件监听器接口
    public interface UIEventListener {
        void onDataChanged();
        void onRefreshRequired();
        Road getSelectedRoad();
        Village getSelectedVillage();
    }

    private final VillageService villageService;
    private final RoadService roadService;
    private final UIEventListener listener;

    public UIEventHandler(VillageService villageService, RoadService roadService, UIEventListener listener) {
        this.villageService = villageService;
        this.roadService = roadService;
        this.listener = listener;
    }

    public void handleAddVillage() {
        Optional<Village> result = showVillageDialog(null, "添加村庄", "请输入村庄信息", "添加");
        result.ifPresent(village -> {
            if (villageService.addVillage(village)) {
                notifyDataChanged();
                AlertUtils.showInformation("添加成功", "村庄添加成功");
            } else {
                AlertUtils.showError("添加失败", "可能ID已存在或数据无效");
            }
        });
    }

    public void handleAddRoad() {
        Optional<Road> result = showRoadDialog(null, "添加道路", "请输入道路信息", "添加");
        result.ifPresent(road -> {
            if (roadService.addRoad(road)) {
                notifyDataChanged();
                AlertUtils.showInformation("添加成功", "道路添加成功");
            } else {
                AlertUtils.showError("添加失败", "可能该路径已存在或数据无效");
            }
        });
    }

    public void handleDeleteVillage() {
        Village selected = listener.getSelectedVillage();
        if (selected == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要删除的村庄");
            return;
        }

        // 检查是否有相关联的道路
        boolean hasConnectedRoads = roadService.getAllRoads().stream()
                .anyMatch(road -> road.getStartId().equals(selected.getId()) ||
                        road.getEndId().equals(selected.getId()));

        boolean confirm;
        if (hasConnectedRoads) {
            confirm = AlertUtils.showConfirmation("确认删除",
                    "删除该村庄将同时删除所有相关联的道路，是否继续？");
        } else {
            confirm = AlertUtils.showConfirmation("确认删除",
                    "确定要删除村庄 " + selected.getName() + " 吗？");
        }
        if (!confirm) return;

        // 执行删除操作
        if (villageService.deleteVillage(selected.getId())) {
            // 删除相关道路
            if (hasConnectedRoads) {
                roadService.getAllRoads().stream()
                        .filter(road -> road.getStartId().equals(selected.getId()) ||
                                road.getEndId().equals(selected.getId()))
                        .forEach(roadService::deleteRoad);
            }

            notifyDataChanged();
            AlertUtils.showInformation("删除成功", "村庄已成功删除");
        } else {
            AlertUtils.showError("删除失败", "无法删除村庄，请稍后重试");
        }
    }

    public void handleDeleteRoad() {
        Road selected = listener.getSelectedRoad();
        if (selected == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要删除的道路");
            return;
        }

        boolean confirm = AlertUtils.showConfirmation("确认删除",
                "确定要删除这条道路吗？");
        if (!confirm) return;

        if (roadService.deleteRoad(selected)) {
            notifyDataChanged();
            AlertUtils.showInformation("删除成功", "道路已成功删除");
        } else {
            AlertUtils.showError("删除失败", "无法删除道路，请稍后重试");
        }
    }

    public void handleEditVillage() {
        Village selectedVillage = listener.getSelectedVillage();
        if (selectedVillage == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要编辑的村庄");
            return;
        }

        Optional<Village> result = showVillageDialog(selectedVillage, "编辑村庄", "修改村庄信息", "保存");
        result.ifPresent(village -> {
            if (villageService.updateVillage(village)) {
                notifyDataChanged();
                AlertUtils.showInformation("更新成功", "村庄信息已更新");
            } else {
                AlertUtils.showError("更新失败", "无法更新村庄信息，请检查数据有效性");
            }
        });
    }

    public void handleEditRoad() {
        Road selectedRoad = listener.getSelectedRoad();
        if (selectedRoad == null) {
            AlertUtils.showWarning("未选择", "请先从表格中选择要编辑的道路");
            return;
        }

        Optional<Road> result = showRoadDialog(selectedRoad, "编辑道路", "修改道路信息", "保存");
        result.ifPresent(road -> {
            road.setLength(MapCalculator.calculateDistance(villageService.getVillageById(road.getStartId()), villageService.getVillageById(road.getEndId())));
            if (roadService.updateRoad(road)) {
                notifyDataChanged();
                AlertUtils.showInformation("更新成功", "道路信息已更新");
            } else {
                AlertUtils.showError("更新失败", "无法更新道路信息，请检查数据有效性");
            }
        });
    }

    /**
     * 显示村庄表单对话框
     * @param village 要编辑的村庄，如果是新增则为null
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param buttonText 确认按钮文本
     * @return 可选的村庄对象
     */
    private Optional<Village> showVillageDialog(Village village, String title, String headerText, String buttonText) {
        // 创建对话框
        Dialog<Village> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);

        // 设置按钮
        ButtonType confirmButton = new ButtonType(buttonText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 初始化控件
        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField xField = new TextField();
        TextField yField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);

        // 如果是编辑模式，填充现有数据
        if (village != null) {
            idField.setText(String.valueOf(village.getId()));
            idField.setDisable(true); // ID不可编辑
            nameField.setText(village.getName());
            xField.setText(String.valueOf(village.getLocateX()));
            yField.setText(String.valueOf(village.getLocateY()));
            descriptionArea.setText(village.getDescription());
        }

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
            if (dialogButton == confirmButton) {
                try {
                    String name = nameField.getText();
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    String description = descriptionArea.getText();

                    Village result = new Village();
                    // 如果是编辑模式，使用原有ID，否则解析用户输入ID
                    if (village != null) {
                        result.setId(village.getId());
                    } else {
                        result.setId(Integer.parseInt(idField.getText()));
                    }

                    result.setName(name);
                    result.setLocateX(x);
                    result.setLocateY(y);
                    result.setDescription(description);

                    return result;
                } catch (NumberFormatException e) {
                    AlertUtils.showError("输入错误", "坐标必须是有效的数字");
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * 显示道路表单对话框
     * @param road 要编辑的道路，如果是新增则为null
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param buttonText 确认按钮文本
     * @return 可选的道路对象
     */
    private Optional<Road> showRoadDialog(Road road, String title, String headerText, String buttonText) {
        // 创建对话框
        Dialog<Road> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);

        // 设置按钮
        ButtonType confirmButton = new ButtonType(buttonText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 村庄选择下拉框
        ComboBox<Village> startVillageCombo = new ComboBox<>();
        ComboBox<Village> endVillageCombo = new ComboBox<>();
        startVillageCombo.getItems().addAll(villageService.getAllVillages());
        endVillageCombo.getItems().addAll(villageService.getAllVillages());

        // 其他字段
        Label idValueLabel = new Label(road != null ? String.valueOf(road.getId()) : "");
        TextField nameField = new TextField();
        TextField lengthField = new TextField();

        // 如果是编辑模式，填充现有数据
        if (road != null) {
            // 获取当前的起点和终点村庄对象
            Village startVillage = villageService.getVillageById(road.getStartId());
            Village endVillage = villageService.getVillageById(road.getEndId());

            // 设置默认选中的村庄
            if (startVillage != null) {
                startVillageCombo.setValue(startVillage);
            }
            if (endVillage != null) {
                endVillageCombo.setValue(endVillage);
            }

            nameField.setText(road.getName());
            lengthField.setText(String.valueOf(road.getLength()));
        }

        // 添加控件到表单
        int row = 0;
        if (road != null) {
            grid.add(new Label("ID:"), 0, row);
            grid.add(idValueLabel, 1, row++);
        }

        grid.add(new Label("起点村庄:"), 0, row);
        grid.add(startVillageCombo, 1, row++);
        grid.add(new Label("终点村庄:"), 0, row);
        grid.add(endVillageCombo, 1, row++);
        grid.add(new Label("道路名称:"), 0, row);
        grid.add(nameField, 1, row++);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButton) {
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

                    double length;
                    Road result;
                    if (road != null && !lengthField.getText().isEmpty()) {
                        // 编辑模式
                        length = Double.parseDouble(lengthField.getText());
                        result = new Road(start.getId(), end.getId(), name, length);
                    } else {
                        // 新增模式
                        result = new Road(start.getId(), end.getId(), name);
                        // 自动计算长度
                        length = MapCalculator.calculateDistance(start, end);
                        result.setLength(length);
                    }

                    if (road != null) {
                        result.setId(road.getId()); // 保留原ID
                    }

                    return result;
                } catch (NumberFormatException e) {
                    AlertUtils.showError("输入错误", "长度必须是有效的数字");
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void notifyDataChanged() {
        if (listener != null) {
            listener.onDataChanged();
            listener.onRefreshRequired();
        }
    }
}