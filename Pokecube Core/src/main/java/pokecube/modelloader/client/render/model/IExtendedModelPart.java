package pokecube.modelloader.client.render.model;

import java.util.HashMap;

import pokecube.core.utils.Vector4;
import thut.api.maths.Vector3;

public interface IExtendedModelPart extends IModelCustom
{
    int[] getRGBAB();

    void setRGBAB(int[] arrays);

    void setPreRotations(Vector4 rotations);

    void setPreTranslations(Vector3 translations);

    void setPostRotations(Vector4 rotations);

    void setPostRotations2(Vector4 rotations);

    void setPostTranslations(Vector3 translations);

    void resetToInit();

    Vector3 getDefaultTranslations();

    Vector4 getDefaultRotations();

    String getName();

    IExtendedModelPart getParent();

    HashMap<String, IExtendedModelPart> getSubParts();

    void addChild(IExtendedModelPart child);

    void setParent(IExtendedModelPart parent);

    String getType();
}
