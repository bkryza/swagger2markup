/*
 * Copyright 2016 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.swagger2markup.internal.utils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.github.swagger2markup.internal.type.*;
import io.swagger.models.*;
import io.swagger.models.properties.Property;
import io.swagger.models.refs.RefFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModelUtils {

    /**
     * Recursively resolve referenced type if {@code type} is of type RefType
     * @param type type to resolve
     * @return referenced type
     */
    public static Type resolveRefType(Type type) {
        if (type == null)
            return null;

        if (type instanceof RefType) {
            System.out.println("Resolving type: "+type.getName()+":"+type.toString());
            return resolveRefType(((RefType) type).getRefType());
        }
        else {
            return type;
        }
    }

    /**
     * Retrieves the type of a model, or otherwise null
     *
     * @param model                      the model
     * @param definitionDocumentResolver the definition document resolver
     * @return the type of the model, or otherwise null
     */
    public static Type getType(Model model, Map<String, Model> definitions, Function<String, String> definitionDocumentResolver) {
        Validate.notNull(model, "model must not be null!");
        System.out.println("Getting type of model: "+model.toString()+":::"+ StringUtils.join(model.getProperties(), ","));
         if (model instanceof ComposedModel) {
            ComposedModel composedModel = (ComposedModel) model;
            Map<String, Property> allProperties = new LinkedHashMap<>();
            ObjectTypePolymorphism polymorphism = new ObjectTypePolymorphism(ObjectTypePolymorphism.Nature.NONE, null);
            String name = model.getTitle();

            if (composedModel.getAllOf() != null) {
                polymorphism.setNature(ObjectTypePolymorphism.Nature.COMPOSITION);

                for (Model innerModel : composedModel.getAllOf()) {
                    Type innerModelType = resolveRefType(getType(innerModel, definitions, definitionDocumentResolver));
                    name = innerModelType.getName();

                    if (innerModelType instanceof ObjectType) {

                        String innerModelDiscriminator = ((ObjectType) innerModelType).getPolymorphism().getDiscriminator();
                        if (innerModelDiscriminator != null) {
                            polymorphism.setNature(ObjectTypePolymorphism.Nature.INHERITANCE);
                            polymorphism.setDiscriminator(innerModelDiscriminator);
                        }

                        Map<String, Property> innerModelProperties = ((ObjectType) innerModelType).getProperties();
                        if (innerModelProperties != null)
                            allProperties.putAll(ImmutableMap.copyOf(innerModelProperties));
                    }
                }
            }
            
            return new ObjectType(name, polymorphism, allProperties);
        }
        else if (model instanceof ModelImpl) {
            ModelImpl modelImpl = (ModelImpl) model;

            if (modelImpl.getAdditionalProperties() != null)
                return new MapType(modelImpl.getTitle(), PropertyUtils.getType(modelImpl.getAdditionalProperties(), definitionDocumentResolver));
            else if (modelImpl.getEnum() != null)
                return new EnumType(modelImpl.getTitle(), modelImpl.getEnum());
            else if (modelImpl.getProperties() != null) {
                ObjectType objectType = new ObjectType(modelImpl.getTitle(), model.getProperties());

                objectType.getPolymorphism().setDiscriminator(modelImpl.getDiscriminator());

                return objectType;
            } else {
                String type = modelImpl.getType();
                String title = modelImpl.getTitle();

                if(type == null) {
                    System.out.println("Warning: Basic type has no type!");
                    type = "object";
                }
                if(title == null) {
                    System.out.println("Warning: Basic type has to title");
                    title = "-inline";
                }

                return new BasicType(type, title);
            }
        }
         else if (model instanceof RefModel) {
            RefModel refModel = (RefModel) model;
            String refName = refModel.getRefFormat().equals(RefFormat.INTERNAL) ? refModel.getSimpleRef() : refModel.getReference();

            Type refType = new ObjectType(refName, null);
            if (definitions.containsKey(refName)) {
                refType = getType(definitions.get(refName), definitions, definitionDocumentResolver);
                refType.setName(refName);
                refType.setUniqueName(refName);
            }

            return new RefType(definitionDocumentResolver.apply(refName), refType);
        } else if (model instanceof ArrayModel) {
            ArrayModel arrayModel = ((ArrayModel) model);

            return new ArrayType(null, PropertyUtils.getType(arrayModel.getItems(), definitionDocumentResolver));
        }

        return null;
    }
}
