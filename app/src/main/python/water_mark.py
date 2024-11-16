import io
from io import BytesIO
from PIL import Image, ImageDraw, ImageFont
import matplotlib.colors as colors  # Ensure this is imported if you use `colors.to_rgba`
from enum import Enum

# Define the enum for watermark positions
class WaterMarkPosition(Enum):
    TOP_RIGHT = 1
    TOP_LEFT = 2
    BOTTOM_RIGHT = 3
    BOTTOM_LEFT = 4
    TOP_CENTER = 5
    BOTTOM_CENTER = 6
    CENTER = 7

def get_position(position_enum, text_width, text_height, image_width, image_height):
    """ Determine the position coordinates based on the WaterMarkPosition enum """
    if position_enum == WaterMarkPosition.TOP_RIGHT:
        return image_width - text_width - 10, 10
    elif position_enum == WaterMarkPosition.TOP_LEFT:
        return 10, 10
    elif position_enum == WaterMarkPosition.BOTTOM_RIGHT:
        return image_width - text_width - 10, image_height - text_height - 10
    elif position_enum == WaterMarkPosition.BOTTOM_LEFT:
        return 10, image_height - text_height - 10
    elif position_enum == WaterMarkPosition.TOP_CENTER:
        return (image_width - text_width) // 2, 10
    elif position_enum == WaterMarkPosition.BOTTOM_CENTER:
        return (image_width - text_width) // 2, image_height - text_height - 10
    elif position_enum == WaterMarkPosition.CENTER:
        return (image_width - text_width) // 2, (image_height - text_height) // 2
    else:
        # Default to bottom-right corner
        return image_width - text_width - 10, image_height - text_height - 10

def extract_rgba(color,opacity):
    if isinstance(color, str):
        try:
            rgba = colors.to_rgba(color)  # Returns a tuple with float values (0-1)
            rgba = tuple(int(c * 255) for c in rgba)  # Convert to integer (0-255)
            return rgba[0], rgba[1], rgba[2], opacity
        except ValueError:
            raise ValueError("Invalid color name. Provide a valid color name or an RGBA tuple.")
    else:
        raise ValueError("Invalid color format. Provide a color name or an RGBA tuple.")

def add_text_watermark_from_bytes(image_bytes, output_path, watermark_text, font_path, text_color, font_size, position, opacity, format='JPEG'):
    # Open the image from bytes
    original = Image.open(io.BytesIO(image_bytes)).convert("RGBA")

    # Extract RGBA color
    text_color_rgba = extract_rgba(text_color,opacity)

    watermark = Image.new("RGBA", original.size, (255, 255, 255, 0))
    draw = ImageDraw.Draw(watermark)

    try:
        font = ImageFont.truetype(io.BytesIO(font_path), font_size)
    except IOError:
        print(f"Error: Font '{font_path}' not found. Please make sure the font file is available.")
        return

    text_width, text_height = draw.textsize(watermark_text, font=font)

    # Get the coordinates based on the provided position
    try:
        position_enum = WaterMarkPosition[position]  # Convert string to enum
    except KeyError:
        print(f"Error: '{position}' is not a valid position. Defaulting to 'BOTTOM_RIGHT'.")
        position_enum = WaterMarkPosition.BOTTOM_RIGHT

    x, y = get_position(position_enum, text_width, text_height, original.width, original.height) if position_enum else get_position(WaterMarkPosition.BOTTOM_RIGHT, text_width, text_height, original.width, original.height)

    # Draw the text on the watermark layer with the specified color
    draw.text((x, y), watermark_text, font=font, fill=text_color_rgba)

    # Combine the watermark with the original image
    watermarked = Image.alpha_composite(original, watermark)

    # Save the result in the specified format
    watermarked.convert("RGB").save(output_path, format=format)
