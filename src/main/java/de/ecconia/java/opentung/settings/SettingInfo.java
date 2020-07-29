package de.ecconia.java.opentung.settings;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface SettingInfo
{
	String key();
	
	String comment() default "";
}
