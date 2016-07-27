
package io.github.swagger2markup.gitbook;

import io.github.swagger2markup.*;
import io.github.swagger2markup.model.*;
import io.github.swagger2markup.spi.*;
import io.github.swagger2markup.markup.builder.*;
import java.io.File;

import static io.github.swagger2markup.utils.IOUtils.normalizeName;

public class OperationExtensions extends PathsDocumentExtension {

    private static final String EXTENSION_ID = "GitbookOperationsExtension";
    private String extensionProperty;
    private String operationsFolder;

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        /* init is executed once */
       // Swagger2MarkupProperties extensionProperties = globalContext.getConfig().getExtensionsProperties();
        //extensionProperty = extensionProperties.getRequiredString(EXTENSION_ID + ".propertyName");
        //Swagger model = globalContext.getSwagger();

        operationsFolder = globalContext.getConfig().getSeparatedOperationsFolder();
    }

    @Override
    public void apply(Context context) {
        MarkupDocBuilder markupBuilder = context.getMarkupDocBuilder(); 
        Position position = context.getPosition();

        if(position == Position.OPERATION_DESCRIPTION_BEFORE && context.getOperation().isPresent()) {
            PathOperation operation = context.getOperation().get();


            if (operationsFolder != null && !operationsFolder.isEmpty()) {
                String targetFile = new File(operationsFolder,
                        markupBuilder.addFileExtension(normalizeName(operation.getId()))).getPath();

                System.out.println("GENERATING LINK TO OPERATION");
                markupBuilder.paragraph("[" + operation.getPath() + "](" + targetFile + ")");
            }
            else {
                System.out.println("GENERATING HASH TO OPERATION");
                markupBuilder.paragraph("[" + operation.getPath() + "]()");
            }
        }
    }

}