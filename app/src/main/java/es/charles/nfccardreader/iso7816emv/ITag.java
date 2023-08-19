package es.charles.nfccardreader.iso7816emv;

import es.charles.nfccardreader.enums.TagTypeEnum;
import es.charles.nfccardreader.enums.TagValueTypeEnum;


public interface ITag {

	enum Class {
		UNIVERSAL, APPLICATION, CONTEXT_SPECIFIC, PRIVATE
	}

	boolean isConstructed();

	byte[] getTagBytes();

	String getName();

	String getDescription();

	TagTypeEnum getType();

	TagValueTypeEnum getTagValueType();

	Class getTagClass();

	int getNumTagBytes();

}
