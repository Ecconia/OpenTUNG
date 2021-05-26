package de.ecconia.java.opentung.settings.keybinds;

import org.lwjgl.glfw.GLFW;

public class GLFWKeyMapper
{
	public static final String[] keyMapping = new String[GLFW.GLFW_KEY_LAST + 1];
	static
	{
		if(GLFW.GLFW_KEY_LAST != 348)
		{
			throw new Error("GLFW library updated. The keymapping does no longer match.");
		}
		
		keyMapping[GLFW.GLFW_KEY_SPACE] = "SPACE";
		keyMapping[GLFW.GLFW_KEY_APOSTROPHE] = "APOSTROPHE";
		keyMapping[GLFW.GLFW_KEY_COMMA] = "COMMA";
		keyMapping[GLFW.GLFW_KEY_MINUS] = "MINUS";
		keyMapping[GLFW.GLFW_KEY_PERIOD] = "PERIOD";
		keyMapping[GLFW.GLFW_KEY_SLASH] = "SLASH";
		keyMapping[GLFW.GLFW_KEY_0] = "'0'";
		keyMapping[GLFW.GLFW_KEY_1] = "'1'";
		keyMapping[GLFW.GLFW_KEY_2] = "'2'";
		keyMapping[GLFW.GLFW_KEY_3] = "'3'";
		keyMapping[GLFW.GLFW_KEY_4] = "'4'";
		keyMapping[GLFW.GLFW_KEY_5] = "'5'";
		keyMapping[GLFW.GLFW_KEY_6] = "'6'";
		keyMapping[GLFW.GLFW_KEY_7] = "'7'";
		keyMapping[GLFW.GLFW_KEY_8] = "'8'";
		keyMapping[GLFW.GLFW_KEY_9] = "'9'";
		keyMapping[GLFW.GLFW_KEY_SEMICOLON] = "SEMICOLON";
		keyMapping[GLFW.GLFW_KEY_EQUAL] = "EQUAL";
		keyMapping[GLFW.GLFW_KEY_A] = "A";
		keyMapping[GLFW.GLFW_KEY_B] = "B";
		keyMapping[GLFW.GLFW_KEY_C] = "C";
		keyMapping[GLFW.GLFW_KEY_D] = "D";
		keyMapping[GLFW.GLFW_KEY_E] = "E";
		keyMapping[GLFW.GLFW_KEY_F] = "F";
		keyMapping[GLFW.GLFW_KEY_G] = "G";
		keyMapping[GLFW.GLFW_KEY_H] = "H";
		keyMapping[GLFW.GLFW_KEY_I] = "I";
		keyMapping[GLFW.GLFW_KEY_J] = "J";
		keyMapping[GLFW.GLFW_KEY_K] = "K";
		keyMapping[GLFW.GLFW_KEY_L] = "L";
		keyMapping[GLFW.GLFW_KEY_M] = "M";
		keyMapping[GLFW.GLFW_KEY_N] = "N";
		keyMapping[GLFW.GLFW_KEY_O] = "O";
		keyMapping[GLFW.GLFW_KEY_P] = "P";
		keyMapping[GLFW.GLFW_KEY_Q] = "Q";
		keyMapping[GLFW.GLFW_KEY_R] = "R";
		keyMapping[GLFW.GLFW_KEY_S] = "S";
		keyMapping[GLFW.GLFW_KEY_T] = "T";
		keyMapping[GLFW.GLFW_KEY_U] = "U";
		keyMapping[GLFW.GLFW_KEY_V] = "V";
		keyMapping[GLFW.GLFW_KEY_W] = "W";
		keyMapping[GLFW.GLFW_KEY_X] = "X";
		keyMapping[GLFW.GLFW_KEY_Y] = "Y";
		keyMapping[GLFW.GLFW_KEY_Z] = "Z";
		keyMapping[GLFW.GLFW_KEY_LEFT_BRACKET] = "LEFT_BRACKET";
		keyMapping[GLFW.GLFW_KEY_BACKSLASH] = "BACKSLASH";
		keyMapping[GLFW.GLFW_KEY_RIGHT_BRACKET] = "RIGHT_BRACKET";
		keyMapping[GLFW.GLFW_KEY_GRAVE_ACCENT] = "GRAVE_ACCENT";
		keyMapping[GLFW.GLFW_KEY_WORLD_1] = "WORLD_1";
		keyMapping[GLFW.GLFW_KEY_WORLD_2] = "WORLD_2";
		keyMapping[GLFW.GLFW_KEY_ESCAPE] = "ESCAPE";
		keyMapping[GLFW.GLFW_KEY_ENTER] = "ENTER";
		keyMapping[GLFW.GLFW_KEY_TAB] = "TAB";
		keyMapping[GLFW.GLFW_KEY_BACKSPACE] = "BACKSPACE";
		keyMapping[GLFW.GLFW_KEY_INSERT] = "INSERT";
		keyMapping[GLFW.GLFW_KEY_DELETE] = "DELETE";
		keyMapping[GLFW.GLFW_KEY_RIGHT] = "RIGHT";
		keyMapping[GLFW.GLFW_KEY_LEFT] = "LEFT";
		keyMapping[GLFW.GLFW_KEY_DOWN] = "DOWN";
		keyMapping[GLFW.GLFW_KEY_UP] = "UP";
		keyMapping[GLFW.GLFW_KEY_PAGE_UP] = "PAGE_UP";
		keyMapping[GLFW.GLFW_KEY_PAGE_DOWN] = "PAGE_DOWN";
		keyMapping[GLFW.GLFW_KEY_HOME] = "HOME";
		keyMapping[GLFW.GLFW_KEY_END] = "END";
		keyMapping[GLFW.GLFW_KEY_CAPS_LOCK] = "CAPS_LOCK";
		keyMapping[GLFW.GLFW_KEY_SCROLL_LOCK] = "SCROLL_LOCK";
		keyMapping[GLFW.GLFW_KEY_NUM_LOCK] = "NUM_LOCK";
		keyMapping[GLFW.GLFW_KEY_PRINT_SCREEN] = "PRINT_SCREEN";
		keyMapping[GLFW.GLFW_KEY_PAUSE] = "PAUSE";
		keyMapping[GLFW.GLFW_KEY_F1] = "F1";
		keyMapping[GLFW.GLFW_KEY_F2] = "F2";
		keyMapping[GLFW.GLFW_KEY_F3] = "F3";
		keyMapping[GLFW.GLFW_KEY_F4] = "F4";
		keyMapping[GLFW.GLFW_KEY_F5] = "F5";
		keyMapping[GLFW.GLFW_KEY_F6] = "F6";
		keyMapping[GLFW.GLFW_KEY_F7] = "F7";
		keyMapping[GLFW.GLFW_KEY_F8] = "F8";
		keyMapping[GLFW.GLFW_KEY_F9] = "F9";
		keyMapping[GLFW.GLFW_KEY_F10] = "F10";
		keyMapping[GLFW.GLFW_KEY_F11] = "F11";
		keyMapping[GLFW.GLFW_KEY_F12] = "F12";
		keyMapping[GLFW.GLFW_KEY_F13] = "F13";
		keyMapping[GLFW.GLFW_KEY_F14] = "F14";
		keyMapping[GLFW.GLFW_KEY_F15] = "F15";
		keyMapping[GLFW.GLFW_KEY_F16] = "F16";
		keyMapping[GLFW.GLFW_KEY_F17] = "F17";
		keyMapping[GLFW.GLFW_KEY_F18] = "F18";
		keyMapping[GLFW.GLFW_KEY_F19] = "F19";
		keyMapping[GLFW.GLFW_KEY_F20] = "F20";
		keyMapping[GLFW.GLFW_KEY_F21] = "F21";
		keyMapping[GLFW.GLFW_KEY_F22] = "F22";
		keyMapping[GLFW.GLFW_KEY_F23] = "F23";
		keyMapping[GLFW.GLFW_KEY_F24] = "F24";
		keyMapping[GLFW.GLFW_KEY_F25] = "F25";
		keyMapping[GLFW.GLFW_KEY_KP_0] = "KP_0";
		keyMapping[GLFW.GLFW_KEY_KP_1] = "KP_1";
		keyMapping[GLFW.GLFW_KEY_KP_2] = "KP_2";
		keyMapping[GLFW.GLFW_KEY_KP_3] = "KP_3";
		keyMapping[GLFW.GLFW_KEY_KP_4] = "KP_4";
		keyMapping[GLFW.GLFW_KEY_KP_5] = "KP_5";
		keyMapping[GLFW.GLFW_KEY_KP_6] = "KP_6";
		keyMapping[GLFW.GLFW_KEY_KP_7] = "KP_7";
		keyMapping[GLFW.GLFW_KEY_KP_8] = "KP_8";
		keyMapping[GLFW.GLFW_KEY_KP_9] = "KP_9";
		keyMapping[GLFW.GLFW_KEY_KP_DECIMAL] = "KP_DECIMAL";
		keyMapping[GLFW.GLFW_KEY_KP_DIVIDE] = "KP_DIVIDE";
		keyMapping[GLFW.GLFW_KEY_KP_MULTIPLY] = "KP_MULTIPLY";
		keyMapping[GLFW.GLFW_KEY_KP_SUBTRACT] = "KP_SUBTRACT";
		keyMapping[GLFW.GLFW_KEY_KP_ADD] = "KP_ADD";
		keyMapping[GLFW.GLFW_KEY_KP_ENTER] = "KP_ENTER";
		keyMapping[GLFW.GLFW_KEY_KP_EQUAL] = "KP_EQUAL";
		keyMapping[GLFW.GLFW_KEY_LEFT_SHIFT] = "LEFT_SHIFT";
		keyMapping[GLFW.GLFW_KEY_LEFT_CONTROL] = "LEFT_CONTROL";
		keyMapping[GLFW.GLFW_KEY_LEFT_ALT] = "LEFT_ALT";
		keyMapping[GLFW.GLFW_KEY_LEFT_SUPER] = "LEFT_SUPER";
		keyMapping[GLFW.GLFW_KEY_RIGHT_SHIFT] = "RIGHT_SHIFT";
		keyMapping[GLFW.GLFW_KEY_RIGHT_CONTROL] = "RIGHT_CONTROL";
		keyMapping[GLFW.GLFW_KEY_RIGHT_ALT] = "RIGHT_ALT";
		keyMapping[GLFW.GLFW_KEY_RIGHT_SUPER] = "RIGHT_SUPER";
		keyMapping[GLFW.GLFW_KEY_MENU] = "MENU";
	}
	
	public static int resolveKeyName(String name)
	{
		for(int i = 0; i < keyMapping.length; i++)
		{
			if(name.equals(keyMapping[i]))
			{
				return i;
			}
		}
		return -1;
	}
}
