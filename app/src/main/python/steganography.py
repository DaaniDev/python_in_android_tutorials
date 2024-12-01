from PIL import Image
from io import BytesIO

# Function to hide text with a key in a byte array image
def hide_text_with_key_bytearray(image_bytes, secret_message, secret_key):
    # Load image from byte array
    img = Image.open(BytesIO(image_bytes))
    encoded = img.copy()
    width, height = img.size

    # Append key to the message
    message_with_key = f"{secret_key}:{secret_message}END"
    print(f"Message to hide: {message_with_key}")  # Debugging line
    binary_message = ''.join([format(ord(i), '08b') for i in message_with_key])
    print(f"Binary message: {binary_message}")  # Debugging line
    binary_iter = iter(binary_message)

    # Check if the image has enough space to hide the message
    required_pixels = len(binary_message)
    available_pixels = width * height * 3  # 3 channels: RGB
    if required_pixels > available_pixels:
        raise ValueError("Image is too small to hide the entire message.")

    # Embed the message into the image's LSB
    for y in range(height):
        for x in range(width):
            pixel = list(img.getpixel((x, y)))
            for i in range(3):  # Modify RGB channels
                try:
                    # Set the LSB to the current bit of the message
                    pixel[i] = pixel[i] & ~1 | int(next(binary_iter))
                except StopIteration:
                    break
            encoded.putpixel((x, y), tuple(pixel))  # Update the pixel with modified value

    # Save the encoded image into a byte array
    output_buffer = BytesIO()
    encoded.save(output_buffer, format="PNG")
    return output_buffer.getvalue()  # Return the byte array of the encoded image

def extract_text_with_key_bytearray(image_bytes):
    # Load image from byte array
    img = Image.open(BytesIO(image_bytes))
    width, height = img.size

    binary_message = ""
    for y in range(height):
        for x in range(width):
            pixel = img.getpixel((x, y))
            for i in range(3):  # Read from RGB channels
                binary_message += str(pixel[i] & 1)

    # Convert binary to string
    message_bits = [binary_message[i:i + 8] for i in range(0, len(binary_message), 8)]
    message = ''.join([chr(int(b, 2)) for b in message_bits])

    # Extract message and key
    if "END" in message:
        parts = message.split("END")[0].split(":")
        if len(parts) == 2:
            return (parts[0], parts[1])
    return None
