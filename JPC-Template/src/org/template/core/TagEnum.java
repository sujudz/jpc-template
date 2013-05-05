package org.template.core;

public enum TagEnum {
	
	/*
	 *LCB left Curly braces 
	 *RCB right Curly braces
	 * */
	 ANNO("\\"), RCB("}"), LCB("{"), INCLUDE("include"),
	 VAR("var"), IF("if"), ELSE("else"), FOR("for"), DOLL("$"), EMPTY("");
	
	/** enum tag */
	private String name;
	
	TagEnum(String name)
	{
		this.name = name;
	}
	
	/*
	 * @param    taginfo Match the class label information
	 */
	public static TagEnum conver(TemplateTagInfo taginfo)
	{
		for (TagEnum tag: TagEnum.values()) {
			if (tag.name.equals(taginfo.tag) 
					|| tag.name.equals(taginfo.anno) 
					|| tag.name.equals(taginfo.cod)) {
				return tag;
			}
		}
		//not match return empty
		return EMPTY;
	}
}
