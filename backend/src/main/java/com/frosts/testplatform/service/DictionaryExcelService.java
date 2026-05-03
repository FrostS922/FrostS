package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.DictionaryItem;
import com.frosts.testplatform.entity.DictionaryType;
import com.frosts.testplatform.repository.DictionaryItemRepository;
import com.frosts.testplatform.repository.DictionaryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DictionaryExcelService {

    private final DictionaryTypeRepository typeRepository;
    private final DictionaryItemRepository itemRepository;

    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        int typeCount = 0;
        int itemCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            // Sheet1: 字典分类
            Sheet typeSheet = workbook.getSheet("DictionaryTypes");
            if (typeSheet != null) {
                Map<String, Long> typeCodeToId = new HashMap<>();

                for (int i = 1; i <= typeSheet.getLastRowNum(); i++) {
                    Row row = typeSheet.getRow(i);
                    if (row == null) continue;

                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String parentCode = getCellValue(row.getCell(2));
                    String description = getCellValue(row.getCell(3));
                    Integer sortOrder = parseInt(getCellValue(row.getCell(4)));
                    Boolean enabled = parseBoolean(getCellValue(row.getCell(5)));

                    if (code == null || name == null) continue;

                    Long parentId = parentCode != null ?
                            typeRepository.findByCodeAndIsDeletedFalse(parentCode)
                                    .map(DictionaryType::getId).orElse(null) : null;

                    DictionaryType type = typeRepository.findByCodeAndIsDeletedFalse(code).orElse(null);
                    if (type == null) {
                        type = new DictionaryType();
                        type.setCode(code.toUpperCase());
                        type.setParentId(parentId);
                        type.setName(name);
                        type.setDescription(description);
                        type.setSortOrder(sortOrder != null ? sortOrder : 0);
                        type.setEnabled(enabled != null ? enabled : true);
                        type = typeRepository.save(type);
                        typeCount++;
                    }
                    typeCodeToId.put(code, type.getId());
                }

                // 处理其他Sheet: 枚举值
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    if (!sheetName.startsWith("Items_")) continue;

                    String typeCode = sheetName.substring(6);
                    Long typeId = typeCodeToId.get(typeCode);
                    if (typeId == null) {
                        typeId = typeRepository.findByCodeAndIsDeletedFalse(typeCode)
                                .map(DictionaryType::getId).orElse(null);
                    }
                    if (typeId == null) continue;

                    for (int j = 1; j <= sheet.getLastRowNum(); j++) {
                        Row row = sheet.getRow(j);
                        if (row == null) continue;

                        String itemCode = getCellValue(row.getCell(0));
                        String itemName = getCellValue(row.getCell(1));
                        String value = getCellValue(row.getCell(2));
                        String itemDesc = getCellValue(row.getCell(3));
                        Integer sortOrder = parseInt(getCellValue(row.getCell(4)));
                        Boolean enabled = parseBoolean(getCellValue(row.getCell(5)));
                        Boolean isDefault = parseBoolean(getCellValue(row.getCell(6)));
                        String color = getCellValue(row.getCell(7));

                        if (itemCode == null || itemName == null) continue;

                        if (!itemRepository.existsByTypeIdAndCodeAndIsDeletedFalse(typeId, itemCode)) {
                            DictionaryItem item = new DictionaryItem();
                            item.setTypeId(typeId);
                            item.setCode(itemCode);
                            item.setName(itemName);
                            item.setValue(value);
                            item.setDescription(itemDesc);
                            item.setSortOrder(sortOrder != null ? sortOrder : 0);
                            item.setEnabled(enabled != null ? enabled : true);
                            item.setIsDefault(isDefault != null ? isDefault : false);
                            item.setColor(color);
                            itemRepository.save(item);
                            itemCount++;
                        }
                    }
                }
            }
        }

        result.put("typeCount", typeCount);
        result.put("itemCount", itemCount);
        return result;
    }

    public byte[] exportToExcel() throws IOException {
        List<DictionaryType> types = typeRepository.findByIsDeletedFalseOrderBySortOrderAsc();

        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet1: 字典分类
            Sheet typeSheet = workbook.createSheet("DictionaryTypes");
            createTypeHeader(typeSheet);

            int rowNum = 1;
            for (DictionaryType type : types) {
                Row row = typeSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(type.getCode());
                row.createCell(1).setCellValue(type.getName());
                if (type.getParentId() != null) {
                    typeRepository.findByIdAndIsDeletedFalse(type.getParentId())
                            .ifPresent(parent -> row.createCell(2).setCellValue(parent.getCode()));
                }
                row.createCell(3).setCellValue(type.getDescription());
                row.createCell(4).setCellValue(type.getSortOrder());
                row.createCell(5).setCellValue(type.getEnabled() != null && type.getEnabled() ? "启用" : "禁用");
            }

            // 每个分类一个Sheet
            for (DictionaryType type : types) {
                String sheetName = "Items_" + type.getCode();
                Sheet itemSheet = workbook.createSheet(sheetName);
                createItemHeader(itemSheet);

                List<DictionaryItem> items = itemRepository.findByTypeIdAndIsDeletedFalseOrderBySortOrderAsc(type.getId());
                int itemRowNum = 1;
                for (DictionaryItem item : items) {
                    Row row = itemSheet.createRow(itemRowNum++);
                    row.createCell(0).setCellValue(item.getCode());
                    row.createCell(1).setCellValue(item.getName());
                    row.createCell(2).setCellValue(item.getValue());
                    row.createCell(3).setCellValue(item.getDescription());
                    row.createCell(4).setCellValue(item.getSortOrder());
                    row.createCell(5).setCellValue(item.getEnabled() != null && item.getEnabled() ? "启用" : "禁用");
                    row.createCell(6).setCellValue(item.getIsDefault() != null && item.getIsDefault() ? "是" : "否");
                    row.createCell(7).setCellValue(item.getColor());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createTypeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] headers = {"分类编码", "分类名称", "父分类编码", "描述", "排序", "启用状态"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private void createItemHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] headers = {"枚举编码", "枚举名称", "实际值", "描述", "排序", "启用状态", "是否默认", "颜色"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null) return null;
        return "启用".equals(value) || "是".equals(value) || "true".equalsIgnoreCase(value);
    }
}
