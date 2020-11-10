import cv2
import numpy

img = numpy.zeros((400, 400, 3), numpy.uint8)
window_name = 'Image'
text = 'Hello World!'
org1 = (50, 50)
org2 = (50, 100)
org3 = (50, 150)
fontFace1 = cv2.FONT_ITALIC
fontFace2 = cv2.FONT_HERSHEY_PLAIN
fontFace3 = cv2.FONT_HERSHEY_COMPLEX
fontScale = 1

text1 = cv2.putText(img, text, org1, fontFace1, fontScale, (0, 255, 0))
text2 = cv2.putText(img, text, org2, fontFace2, fontScale, (255, 0, 0))
text3 = cv2.putText(img, text, org3, fontFace3, fontScale, (0, 0, 255))

cv2.namedWindow('image', cv2.WINDOW_NORMAL)
cv2.imshow('image', text1)
cv2.imshow('image', text2)
cv2.imshow('image', text3)
cv2.waitKey(0)
cv2.destroyAllWindows()
