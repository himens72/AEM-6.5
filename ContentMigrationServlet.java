package com.brp.aem.nextgen.core.servlets;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.brp.aem.nextgen.core.utils.CreatePackageUtils;
import com.brp.aem.nextgen.core.utils.RequestUtils;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.Rendition;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = Servlet.class,
    property = {
      Constants.SERVICE_DESCRIPTION + "= Content Migration Servlet for Asset Paths",
      "sling.servlet.methods=" + HttpConstants.METHOD_POST,
      "sling.servlet.paths=/bin/contentMigration"
    })
public class ContentMigrationServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 8292727924234484731L;
  private static final Logger logger = LoggerFactory.getLogger(ContentMigrationServlet.class);
  private static final String DAM_PATH = "/content/dam";
  private int row = 0;
  private int SAVE_COUNTER = 200;
  XSSFWorkbook workbook = null;
  XSSFSheet sheet = null;
  XSSFRow xssfRow;
  XSSFCell xssfCell;
  CellStyle cellStyle;
  private File excelFile;
  @Reference private QueryBuilder queryBuilder;
  private Session session;
  private StringBuilder builder = null;
  private PrintWriter printWriter = null;
  boolean dryrun = false;
  boolean rollback = false;
  List<Resource> packageResources;
  Map<String, String> assetMapping = null;

  @Reference private Packaging packaging;

  @Reference private PackageHelper packageHelper;

  @Override
  protected void doPost(
      final SlingHttpServletRequest request, final SlingHttpServletResponse response)
      throws ServletException {
    try {
      printWriter = response.getWriter();
      builder = new StringBuilder();
      responseLog("Content Migration Started ..............");
      responseLog("Utility Start Time : " + getDate());
      ResourceResolver resourceResolver = request.getResourceResolver();
      JSONObject jsonObject = RequestUtils.getJSONPostData(request);
      if (jsonObject != null) {
        String folderPath = jsonObject.getString("folderPaths"),
            reportPath = jsonObject.getString("reportPath"),
            reportName = jsonObject.getString("reportName"),
            packageName = jsonObject.getString("packageName"),
            file = jsonObject.getString("file");
        dryrun = jsonObject.getBoolean("dryrun");
        rollback = jsonObject.getBoolean("rollback");
        parseFile(request, file);
        logger.info("FileName read done");
        if (dryrun) {
          responseLog("Dry Run is Performed");
          reportName = reportName + "-" + "dryrun";
        }
        if (rollback) {
          responseLog("Roll Back is Performed");
          reportName = reportName + "-" + "rollback";
        }
        session = resourceResolver.adaptTo(Session.class);
        responseLog("---------------------------------------------------------");
        responseLog("Report Path : " + reportPath + "/" + reportName);
        if (assetMapping.isEmpty()) {
          responseLog("assetMapping is null for  assets");
        } else {
          responseLog("Number of assetMapping is " + assetMapping.size());
          excuteResults(resourceResolver, folderPath, reportPath, reportName);
          // Create Package Goes here
          if (packageResources.isEmpty() || dryrun) {
            responseLog("No paths there for creating packages or Dryrun is performed");
          } else {
            responseLog("Package creation in Progress");
            CreatePackageUtils.createPackage(
                resourceResolver, packageHelper, packaging, packageResources, packageName);
            responseLog("Package created");
            responseLog("Package name: " + packageName);
          }
          responseLog("Total number of content paths to be replicated " + packageResources.size());
        }

        responseLog("---------------------------------------------------------");
        responseLog("Utility End Time : " + getDate());
        responseLog("Content Migration Successfully Completed");
        printWriter.println(builder.toString());
      } else {
        responseLog("Null JSON object");
      }
    } catch (IOException e) {
      responseLog("Input output exception");
      logger.info(e.getMessage());
      responseLog(e.getMessage());
    } catch (NullPointerException e) {
      responseLog("Null pointer exception");
      logger.info(e.getMessage());
    } catch (JSONException e) {
      responseLog("Error in  reading parameters in JSON object");
      logger.info(e.getMessage());
      responseLog(e.getMessage());
    } catch (Exception e) {
      responseLog("Exception");
      logger.info(e.getMessage());
      responseLog(e.getMessage());
    }
  }

  private synchronized void parseFile(SlingHttpServletRequest request, String file)
      throws IOException {
    InputStream assetMappingInputStream = null;
    XSSFWorkbook assetMappingWorkbook = null;
    try {
      responseLog("Read Mapping excel is Performed");
      Resource fileResource = request.getResourceResolver().getResource(file);
      Asset asset = fileResource.adaptTo(Asset.class);
      if (asset != null) {
        Rendition rendition = asset.getOriginal();
        if (rendition != null) {
          assetMappingInputStream = rendition.getStream();
        }
      }
      String fileExtension = FilenameUtils.getExtension(file);
      if (assetMappingInputStream == null || StringUtils.isEmpty(fileExtension)) {
        responseLog("File null");
        throw new IOException("Importing File not found");
      }
      assetMappingWorkbook = new XSSFWorkbook(assetMappingInputStream);
      // Get first/desired sheet from the workbook
      createMapping(assetMappingWorkbook);
      responseLog("assetMapping Excel read Complete");
    } finally {
      assetMappingWorkbook.close();
      assetMappingInputStream.close();
    }
  }

  private synchronized void createMapping(XSSFWorkbook assetMappingWorkbook) throws IOException {
    XSSFSheet assetMappingSheet = assetMappingWorkbook.getSheetAt(0);
    responseLog("assetMappingSheet is picked");

    assetMapping = new HashMap<String, String>();
    for (int i = assetMappingSheet.getFirstRowNum() + 1;
        i <= assetMappingSheet.getLastRowNum();
        i++) {
      XSSFRow assetMappingRow = assetMappingSheet.getRow(i);
      if (rollback) {
        assetMapping.put(
            assetMappingRow.getCell(1).getStringCellValue(),
            assetMappingRow.getCell(0).getStringCellValue());
      } else {
        assetMapping.put(
            assetMappingRow.getCell(0).getStringCellValue(),
            assetMappingRow.getCell(1).getStringCellValue());
      }
    }
  }

  public void createAsset(ResourceResolver resourceResolver, String reportPath, String reportName) {
    try (InputStream stream = new FileInputStream(excelFile); ) {
      AssetManager manager = resourceResolver.adaptTo(AssetManager.class);
      manager.createAsset(reportPath + "/" + reportName, stream, "application/vnd.ms-excel", true);
      responseLog("Content Migration Report is created in " + reportPath + "/" + reportName);
    } catch (IOException e) {
      logger.error("Unable to Content Migration Report : {}", e.getMessage());
      responseLog("Unable to Content Migration Report : " + e.getMessage());
    }
  }

  protected void excuteResults(
      ResourceResolver resourceResolver, String folderPath, String reportPath, String reportName)
      throws IOException {
    row = 0;
    reportName = reportName + ".xlsx";
    excelFile = new File(reportName);
    configureExcelSheet();
    queryPage(resourceResolver, folderPath);
    try (FileOutputStream outstream = new FileOutputStream(excelFile)) {
      workbook.write(outstream);
      createAsset(resourceResolver, reportPath, reportName);
    } catch (IOException e) {
      responseLog("Unable to write in workbook : " + e.getMessage());
      logger.error(e.getMessage());
    } finally {
      if (excelFile.exists()) {
        excelFile.delete();
      }
      if (workbook != null) {
        workbook.close();
      }
    }
  }

  public void configureExcelSheet() {
    responseLog("Creating excel for report");
    workbook = new XSSFWorkbook();
    sheet = workbook.createSheet("Sheet1");
    sheet.setColumnWidth(0, 9000);
    sheet.setColumnWidth(1, 9000);
    cellStyle = workbook.createCellStyle();
    cellStyle.setWrapText(true);
    xssfRow = sheet.createRow(row);
    writeColumn(xssfRow, 0, "Path");
    writeColumn(xssfRow, 1, "Property");
    writeColumn(xssfRow, 2, "Value");
    writeColumn(xssfRow, 3, "NewValue");
    row++;
  }

  public void writeColumn(XSSFRow xssfRow, int cellColumn, String value) {
    xssfCell = xssfRow.createCell(cellColumn);
    xssfCell.setCellValue(value);
    xssfCell.setCellStyle(cellStyle);
  }

  public void queryPage(ResourceResolver resourceResolver, String folderPath) {
    try {
      responseLog("Inside queryPage method");
      if (StringUtils.isNotBlank(folderPath)) {
        packageResources = new ArrayList<Resource>();
        String[] folderPaths = folderPath.split(";");
        for (int i = 0; i < folderPaths.length; i++) {
          Map<String, String> predicateMap = new HashMap<>();
          predicateMap.put("fulltext", DAM_PATH);
          predicateMap.put("p.limit", "-1");
          predicateMap.put("orderby", "path");
          predicateMap.put("path", folderPaths[i]);
          Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), session);
          SearchResult result = query.getResult();
          responseLog(
              "Number of Content paths for " + folderPaths[i] + " is " + result.getHits().size());
          int count = 1;
          for (Hit hit : result.getHits()) {
            getPageReference(resourceResolver, hit.getPath());
            // save operation after every n nodes or at the end of hit size
            if ((((count % SAVE_COUNTER) == 0) || (count == result.getHits().size() - 1))
                && !dryrun) {
              resourceResolver.commit();
            }
            count++;
          }
        }
      }
    } catch (RepositoryException e) {
      logger.error(e.getMessage());
      responseLog(e.getMessage());
    } catch (PersistenceException e) {
      logger.error(e.getMessage());
      responseLog(e.getMessage());
    }
  }

  public void getPageReference(ResourceResolver resourceResolver, String path) {

    Resource resource = resourceResolver.getResource(path);
    ValueMap valueMap = resource.adaptTo(ValueMap.class);
    for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
      String pageProperty = entry.getKey();
      String value = entry.getValue().toString();
      if (StringUtils.isNotBlank(value) && value.contains(DAM_PATH)) {
        updateProperty(valueMap, pageProperty, value, resource, xssfCell, xssfRow);
      }
    }
  }

  private void updateProperty(
      ValueMap valueMap,
      String pageProperty,
      String value,
      Resource resource,
      XSSFCell xssfCell,
      XSSFRow xssfRow) {
    String updateAssetReference = value;
    int replaceCounter = 0;
    for (Map.Entry<String, String> entry : assetMapping.entrySet()) {
      String assetNewPath = entry.getValue().toString();
      String assetOldPath = entry.getKey();
      if (updateAssetReference.contains(assetOldPath)) {
        updateAssetReference = updateAssetReference.replace(assetOldPath, assetNewPath);
        replaceCounter++;
      }
    }
    if (replaceCounter > 0) {
      if (!dryrun) {
        ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);
        modifiableValueMap.put(pageProperty, updateAssetReference);
      }

      xssfRow = sheet.createRow(row);
      writeColumn(xssfRow, 0, resource.getPath());
      writeColumn(xssfRow, 1, pageProperty);
      writeColumn(xssfRow, 2, value);
      writeColumn(xssfRow, 3, updateAssetReference);
      packageResources.add(resource);
      row++;
    }
  }

  private void responseLog(String string) {
    builder.append(System.lineSeparator());
    builder.append("<br/>");
    builder.append(string);
    logger.info(string);
  }

  private String getDate() {
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    Date date = new Date();
    return formatter.format(date);
  }
}
