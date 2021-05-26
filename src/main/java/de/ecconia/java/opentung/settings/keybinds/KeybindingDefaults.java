package de.ecconia.java.opentung.settings.keybinds;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface KeybindingDefaults
{
	String key();
	
	String defaultValue();
	
	String comment() default "";
}
