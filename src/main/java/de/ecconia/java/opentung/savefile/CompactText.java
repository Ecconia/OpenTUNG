package de.ecconia.java.opentung.savefile;

public class CompactText
{
	public static byte[] encode(String text)
	{
		int letterAmount = text.length();
		byte[] encodedLetter = new byte[letterAmount + 1]; //Always add 1 slot, to grant access.
		char[] textArray = text.toCharArray();
		if(textArray.length != 0 && textArray[textArray.length - 1] == '-')
		{
			throw new IllegalArgumentException("To be encoded String must not end with '-'.");
		}
		for(int i = 0; i < letterAmount; i++)
		{
			char c = textArray[i];
			if(c == '-')
			{
				encodedLetter[i] = 0b0;
			}
			else if(c == '.')
			{
				encodedLetter[i] = 0b1011;
			}
			else if(c >= '0' && c <= '9')
			{
				encodedLetter[i] = (byte) (c - '0' + 0b1);
			}
			else if(c >= 'a' && c <= 'z')
			{
				encodedLetter[i] = (byte) (c - 'a' + 0b1100);
			}
			else if(c >= 'A' && c <= 'Z')
			{
				encodedLetter[i] = (byte) (c - 'A' + 0b100110);
			}
			else
			{
				throw new IllegalArgumentException("Char unsupported by encoding: (" + (int) c + ") '" + c + "'");
			}
		}
		int compressedByteAmount = letterAmount / 4 * 3 + letterAmount % 4;
		byte[] compressedBytes = new byte[compressedByteAmount];
		int cycle = 0;
		int letterIndex = 0;
		for(int i = 0; i < compressedByteAmount; i++)
		{
			if(cycle == 0)
			{
				compressedBytes[i] = (byte) (encodedLetter[letterIndex++] << 2 | encodedLetter[letterIndex] >>> 4 & 0b11);
				cycle++;
			}
			else if(cycle == 1)
			{
				compressedBytes[i] = (byte) (encodedLetter[letterIndex++] << 4 | encodedLetter[letterIndex] >>> 2 & 0b1111);
				cycle++;
			}
			else
			{
				compressedBytes[i] = (byte) (encodedLetter[letterIndex++] << 6 | encodedLetter[letterIndex++] & 0b111111);
				cycle = 0;
			}
		}
		
		return compressedBytes;
	}
	
	public static String decode(byte[] bytes)
	{
		int maxLetterAmount = (int) Math.ceil((float) bytes.length / 3f) * 4;
		byte[] decodedLetters = new byte[maxLetterAmount];
		{
			int letterIndex = 0;
			int cycle = 0;
			for(int i = 0; i < bytes.length; i++)
			{
				int b = bytes[i] & 0xFF;
				if(cycle == 0)
				{
					decodedLetters[letterIndex + 1] = (byte) ((b & 0b11) << 4);
					b >>>= 2;
					decodedLetters[letterIndex] |= (byte) b;
					letterIndex += 1;
					cycle = 1;
				}
				else if(cycle == 1)
				{
					decodedLetters[letterIndex + 1] = (byte) ((b & 0b1111) << 2);
					b >>>= 4;
					decodedLetters[letterIndex] |= (byte) b;
					letterIndex += 1;
					cycle = 2;
				}
				else
				{
					decodedLetters[letterIndex + 1] = (byte) (b & 0b111111);
					b >>>= 6;
					decodedLetters[letterIndex] |= (byte) b;
					letterIndex += 2;
					cycle = 0;
				}
			}
		}
		for(int i = 0; i < decodedLetters.length; i++)
		{
			int b = decodedLetters[i] & 0b111111;
			if(b == 0b0)
			{
				decodedLetters[i] = '-';
			}
			else if(b <= 0b1010)
			{
				decodedLetters[i] = (byte) (b - 0b1 + '0');
			}
			else if(b == 0b1011)
			{
				decodedLetters[i] = '.';
			}
			else if(b <= 0b100101)
			{
				decodedLetters[i] = (byte) (b - 0b1100 + 'a');
			}
			else
			{
				decodedLetters[i] = (byte) (b - 0b100110 + 'A');
			}
		}
		
		int to = decodedLetters.length;
		int max = to < 4 ? to : 4;
		for(int i = 0; i < max; i++)
		{
			if(decodedLetters[to - 1] == '-')
			{
				to--;
			}
			else
			{
				break;
			}
		}
		
		return new String(decodedLetters, 0, to);
	}
}
