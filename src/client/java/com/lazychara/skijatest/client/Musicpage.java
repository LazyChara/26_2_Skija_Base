package com.lazychara.skijatest.client;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterBlurMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.MaskFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Musicpage extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int W88 = 0xE0FFFFFF;
    private static final int W70 = 0xB3FFFFFF;
    private static final int W46 = 0x75FFFFFF;
    private static final int W30 = 0x4DFFFFFF;
    private static final int W18 = 0x2EFFFFFF;
    private static final int W12 = 0x1FFFFFFF;
    private static final int AMLL_VOLUME = 0x80FFFFFF;
    private static final int AMLL_VOLUME_FILL = 0x66FFFFFF;
    private static final int AMLL_VOLUME_BG = 0x26FFFFFF;
    private static final int AMLL_ACTIVE_BG = 0xE6FFFFFF;
    private static final int AMLL_ACTIVE_ICON = 0xCC000000;
    private static final int AMLL_BUTTON_HOVER = 0x24FFFFFF;
    private static final long ENTRY_ANIM_MS = 420L;
    private static final long RETURN_ANIM_MS = 340L;
    private static final long CLOSE_ANIM_MS = 260L;
    private static final long BG_INTERVAL_PLAYING_MS = 66L;
    private static final long BG_INTERVAL_IDLE_MS = 160L;
    private static final long BG_INTERVAL_EXIT_MS = 120L;
    private static final long DRAG_RENDER_INTERVAL_MS = 40L;
    private static final float BG_RENDER_SCALE = 0.50f;
    private static final int BG_RENDER_MIN_W = 360;
    private static final int BG_RENDER_MIN_H = 202;
    private static final int BG_RENDER_MAX_W = 1080;
    private static final int BG_RENDER_MAX_H = 660;
    private static final float LYRIC_OVERFLOW_PENALTY_MULTIPLIER = 1000f;
    private static final float LYRIC_CJK_BREAK_PENALTY_RATIO = 0.15f;
    private static final float LYRIC_NORMAL_BREAK_PENALTY_RATIO = 0.50f;
    private static final float LYRIC_SPACE_BREAK_REWARD_RATIO = 0.40f;
    private static final float LYRIC_PUNCTUATION_BREAK_REWARD_RATIO = 0.60f;
    private static final String LYRIC_BREAK_PUNCTUATION = ",.;:!?，。；：！？、）】》」』’”)[\\]}>~…";
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]\\s*(.*)");
    private static final Pattern LYRIC_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final String ICON_PLAY = "M5.80762 32.4896V5.4925C5.80762 4.305 6.12305 3.41438 6.75391 2.82063C7.38477 2.22688 8.13932 1.93 9.01758 1.93C9.78451 1.93 10.5391 2.14029 11.2812 2.56086L33.7324 15.6605C34.5859 16.1553 35.223 16.6562 35.6436 17.1634C36.0641 17.6582 36.2744 18.2705 36.2744 19.0003C36.2744 19.7054 36.0641 20.3177 35.6436 20.8372C35.223 21.3444 34.5859 21.8392 33.7324 22.3216L11.2812 35.4212C10.5391 35.8542 9.78451 36.0706 9.01758 36.0706C8.13932 36.0706 7.38477 35.7676 6.75391 35.1614C6.12305 34.5677 5.80762 33.6771 5.80762 32.4896Z";
    private static final String ICON_PAUSE = "M8.46953 37C7.37801 37 6.56603 36.7271 6.03359 36.1814C5.51445 35.6489 5.25488 34.8502 5.25488 33.7854V4.21464C5.25488 3.14975 5.52111 2.35108 6.05355 1.81864C6.59931 1.27288 7.40463 1 8.46953 1H13.3813C14.4329 1 15.2249 1.27288 15.7574 1.81864C16.3031 2.35108 16.576 3.14975 16.576 4.21464V33.7854C16.576 34.8502 16.3031 35.6489 15.7574 36.1814C15.2249 36.7271 14.4329 37 13.3813 37H8.46953ZM24.6426 37C23.5644 37 22.759 36.7271 22.2266 36.1814C21.6942 35.6489 21.4279 34.8502 21.4279 33.7854V4.21464C21.4279 3.14975 21.6942 2.35108 22.2266 1.81864C22.7724 1.27288 23.5777 1 24.6426 1H29.5544C30.6193 1 31.4179 1.27288 31.9504 1.81864C32.4828 2.35108 32.7491 3.14975 32.7491 4.21464V33.7854C32.7491 34.8502 32.4828 35.6489 31.9504 36.1814C31.4179 36.7271 30.6193 37 29.5544 37H24.6426Z";
    private static final String ICON_FORWARD_LEFT = "M62 60.0717C65.938 62.3453 67.9069 63.4821 68.5677 64.9662C69.1441 66.2608 69.1441 67.7391 68.5677 69.0336C67.9069 70.5177 65.938 71.6545 62 73.9281L41 86.0525C37.062 88.326 35.0931 89.4628 33.4774 89.293C32.0681 89.1449 30.7878 88.4057 29.9549 87.2593C29 85.945 29 83.6714 29 79.1243V54.8755C29 50.3284 29 48.0548 29.9549 46.7405C30.7878 45.5941 32.0681 44.8549 33.4774 44.7068C35.0931 44.537 37.062 45.6738 41 47.9473L62 60.0717Z";
    private static final String ICON_FORWARD_RIGHT = "M102 60.0717C105.938 62.3453 107.907 63.4821 108.568 64.9662C109.144 66.2608 109.144 67.7391 108.568 69.0336C107.907 70.5177 105.938 71.6545 102 73.9281L81 86.0525C77.062 88.326 75.0931 89.4628 73.4774 89.293C72.0681 89.1449 70.7878 88.4057 69.9549 87.2593C69 85.945 69 83.6714 69 79.1243V54.8755C69 50.3284 69 48.0548 69.9549 46.7405C70.7878 45.5941 72.0681 44.8549 73.4774 44.7068C75.0931 44.537 77.062 45.6738 81 47.9473L102 60.0717Z";
    private static final String ICON_REWIND_RIGHT = "M72 60.0717C68.062 62.3453 66.0931 63.4821 65.4323 64.9662C64.8559 66.2608 64.8559 67.7391 65.4323 69.0336C66.0931 70.5177 68.062 71.6545 72 73.9281L93 86.0525C96.938 88.326 98.9069 89.4628 100.523 89.293C101.932 89.1449 103.212 88.4057 104.045 87.2593C105 85.945 105 83.6714 105 79.1243V54.8755C105 50.3284 105 48.0548 104.045 46.7405C103.212 45.5941 101.932 44.8549 100.523 44.7068C98.9069 44.537 96.938 45.6738 93 47.9473L72 60.0717Z";
    private static final String ICON_REWIND_LEFT = "M32 60.0717C28.062 62.3453 26.0931 63.4821 25.4323 64.9662C24.8559 66.2608 24.8559 67.7391 25.4323 69.0336C26.0931 70.5177 28.062 71.6545 32 73.9281L53 86.0525C56.938 88.326 58.9069 89.4628 60.5226 89.293C61.9319 89.1449 63.2122 88.4057 64.0451 87.2593C65 85.945 65 83.6714 65 79.1243V54.8755C65 50.3284 65 48.0548 64.0451 46.7405C63.2122 45.5941 61.9319 44.8549 60.5226 44.7068C58.9069 44.537 56.938 45.6738 53 47.9473L32 60.0717Z";
    private static final String ICON_SHUFFLE = "M10.624 36.3125C10.624 35.75 10.8218 35.2754 11.2173 34.8887C11.6216 34.4932 12.1094 34.2954 12.6807 34.2954H15.4756C16.3896 34.2954 17.1455 34.1372 17.7432 33.8208C18.3496 33.5044 18.9341 32.9946 19.4966 32.2915L27.3936 22.3379C28.3955 21.0811 29.4282 20.2285 30.4917 19.7803C31.5552 19.332 32.79 19.1079 34.1963 19.1079H36.4243V16.1548C36.4243 15.6714 36.5605 15.2935 36.833 15.021C37.1055 14.7397 37.479 14.5991 37.9536 14.5991C38.1821 14.5991 38.3843 14.6343 38.5601 14.7046C38.7446 14.7749 38.9072 14.8672 39.0479 14.9814L44.8223 19.8857C45.1826 20.1846 45.3628 20.5493 45.3628 20.98C45.3628 21.4106 45.1826 21.7754 44.8223 22.0742L39.0479 26.9917C38.9072 27.106 38.7446 27.2026 38.5601 27.2817C38.3843 27.3521 38.1821 27.3872 37.9536 27.3872C37.479 27.3872 37.1055 27.2466 36.833 26.9653C36.5605 26.6841 36.4243 26.3018 36.4243 25.8184V23.1421H33.9194C33.3218 23.1421 32.8076 23.2036 32.377 23.3267C31.9551 23.4497 31.564 23.6562 31.2036 23.9463C30.8521 24.2275 30.4829 24.6143 30.0962 25.1064L21.606 35.7061C20.8853 36.6113 20.0986 37.2749 19.2461 37.6968C18.3936 38.1099 17.3389 38.3164 16.082 38.3164H12.6807C12.1094 38.3164 11.6216 38.123 11.2173 37.7363C10.8218 37.3496 10.624 36.875 10.624 36.3125ZM10.624 21.125C10.624 20.5625 10.8218 20.0879 11.2173 19.7012C11.6216 19.3057 12.1094 19.1079 12.6807 19.1079H15.7261C16.9829 19.1079 18.0947 19.3188 19.0615 19.7407C20.0371 20.1538 20.8853 20.8174 21.606 21.7314L30.0435 32.2783C30.5972 32.9727 31.1992 33.4824 31.8496 33.8076C32.5 34.1328 33.291 34.2954 34.2227 34.2954H36.4243V31.5664C36.4243 31.083 36.5605 30.7007 36.833 30.4194C37.1055 30.1382 37.479 29.9976 37.9536 29.9976C38.1821 29.9976 38.3843 30.0371 38.5601 30.1162C38.7446 30.1865 38.9072 30.2832 39.0479 30.4062L44.8223 35.2974C45.1826 35.5962 45.3628 35.9609 45.3628 36.3916C45.3628 36.8223 45.1826 37.187 44.8223 37.4858L39.0479 42.3901C38.9072 42.5132 38.7446 42.6099 38.5601 42.6802C38.3843 42.7593 38.1821 42.7988 37.9536 42.7988C37.479 42.7988 37.1055 42.6582 36.833 42.377C36.5605 42.0957 36.4243 41.7134 36.4243 41.23V38.3164H34.1699C32.9043 38.3164 31.7222 38.1011 30.6235 37.6704C29.5249 37.231 28.5625 36.4927 27.7363 35.4556L19.4966 25.146C18.9341 24.4429 18.2925 23.9331 17.5718 23.6167C16.8599 23.3003 16.0381 23.1421 15.1064 23.1421H12.6807C12.1094 23.1421 11.6216 22.9443 11.2173 22.5488C10.8218 22.1533 10.624 21.6787 10.624 21.125Z";
    private static final String ICON_REPEAT = "M14.2495 28.9956C13.6519 28.9956 13.1465 28.7891 12.7334 28.376C12.3203 27.9541 12.1138 27.4531 12.1138 26.873V25.3438C12.1138 23.832 12.4565 22.5312 13.1421 21.4414C13.8276 20.3516 14.8076 19.5166 16.082 18.9365C17.3564 18.3477 18.877 18.0532 20.6436 18.0532H30.3599V15.4033C30.3599 14.9111 30.4961 14.5288 30.7686 14.2563C31.041 13.9751 31.4146 13.8345 31.8892 13.8345C32.1177 13.8345 32.3198 13.874 32.4956 13.9531C32.6714 14.0234 32.8296 14.1113 32.9702 14.2168L38.7578 19.1343C39.1182 19.4331 39.2939 19.7979 39.2852 20.2285C39.2852 20.6504 39.1094 21.0107 38.7578 21.3096L32.9702 26.2271C32.8296 26.3501 32.6714 26.4468 32.4956 26.5171C32.3198 26.5874 32.1177 26.6226 31.8892 26.6226C31.4146 26.6226 31.041 26.4819 30.7686 26.2007C30.4961 25.9194 30.3599 25.5415 30.3599 25.0669V22.1929H20.459C19.1846 22.1929 18.1826 22.5269 17.4531 23.1948C16.7236 23.8628 16.3589 24.7812 16.3589 25.9502V26.873C16.3589 27.4531 16.1523 27.9541 15.7393 28.376C15.3262 28.7891 14.8296 28.9956 14.2495 28.9956ZM41.7505 26.7017C42.3306 26.7017 42.8271 26.9082 43.2402 27.3213C43.6621 27.7344 43.873 28.2354 43.873 28.8242V30.3535C43.873 31.8652 43.5303 33.166 42.8447 34.2559C42.1592 35.3457 41.1792 36.1851 39.9048 36.7739C38.6304 37.354 37.1055 37.644 35.3301 37.644H25.627V40.2676C25.627 40.751 25.4907 41.1333 25.2183 41.4146C24.9458 41.6958 24.5723 41.8364 24.0977 41.8364C23.8691 41.8364 23.6626 41.7969 23.478 41.7178C23.3022 41.6475 23.1484 41.5552 23.0166 41.4409L17.2158 36.5366C16.873 36.2466 16.6973 35.8862 16.6885 35.4556C16.6885 35.0249 16.8643 34.6558 17.2158 34.3481L23.0166 29.4307C23.1484 29.3164 23.3022 29.2241 23.478 29.1538C23.6626 29.0835 23.8691 29.0483 24.0977 29.0483C24.5723 29.0483 24.9458 29.189 25.2183 29.4702C25.4907 29.7427 25.627 30.125 25.627 30.6172V33.4912H35.5278C36.8022 33.4912 37.8042 33.1616 38.5337 32.5024C39.2632 31.8345 39.6279 30.916 39.6279 29.7471V28.8242C39.6279 28.2354 39.8301 27.7344 40.2344 27.3213C40.6475 26.9082 41.1528 26.7017 41.7505 26.7017Z";
    private static final String ICON_SPEAKER = "M14.9042 27.1802C14.4202 27.1802 14.0473 26.9897 13.595 26.5612L10.3815 23.5461C10.3339 23.5065 10.2863 23.4906 10.2228 23.4906H8.01703C6.70778 23.4906 5.99365 22.7527 5.99365 21.38V18.4442C5.99365 17.0715 6.70778 16.3257 8.01703 16.3257H10.2307C10.2863 16.3257 10.3418 16.3019 10.3815 16.2622L13.595 13.2709C14.079 12.8107 14.4361 12.6282 14.8883 12.6282C15.6104 12.6282 16.142 13.1915 16.142 13.8977V25.9344C16.142 26.6406 15.6104 27.1802 14.9042 27.1802Z";
    private static final String ICON_SPEAKER3_BODY = "M24.0403 27.1802C23.5642 27.1802 23.1913 26.9897 22.739 26.5612L19.5176 23.5461C19.4779 23.5065 19.4224 23.4906 19.3668 23.4906H17.161C15.8518 23.4906 15.1377 22.7527 15.1377 21.38V18.4442C15.1377 17.0715 15.8518 16.3257 17.161 16.3257H19.3668C19.4303 16.3257 19.4779 16.3019 19.5255 16.2622L22.739 13.2709C23.223 12.8107 23.5721 12.6282 24.0324 12.6282C24.7544 12.6282 25.286 13.1915 25.286 13.8977V25.9344C25.286 26.6406 24.7544 27.1802 24.0403 27.1802Z";
    private static final String ICON_SPEAKER3_W1 = "M28.0948 23.6653C27.6028 23.3559 27.4996 22.7687 27.8964 22.1101C28.2931 21.4991 28.5232 20.7136 28.5232 19.8964C28.5232 19.0712 28.301 18.2856 27.8964 17.6826C27.4917 17.032 27.6028 16.4369 28.0948 16.1274C28.547 15.8418 29.1104 15.9529 29.404 16.3576C30.0863 17.3097 30.491 18.5713 30.491 19.8964C30.491 21.2214 30.0863 22.4831 29.404 23.4273C29.1104 23.8399 28.547 23.943 28.0948 23.6653Z";
    private static final String ICON_SPEAKER3_W2 = "M31.6733 25.8711C31.1576 25.5696 31.0942 24.9428 31.4432 24.3794C32.2526 23.1257 32.7207 21.5468 32.7207 19.8964C32.7207 18.2459 32.2605 16.6591 31.4432 15.4133C31.0942 14.8499 31.1576 14.2231 31.6733 13.9137C32.1415 13.6439 32.7128 13.755 33.0143 14.2152C34.0855 15.7783 34.6885 17.8016 34.6885 19.8964C34.6885 21.9911 34.0775 23.9985 33.0143 25.5775C32.7128 26.0377 32.1415 26.1488 31.6733 25.8711Z";
    private static final String ICON_SPEAKER3_W3 = "M35.2362 28.1007C34.7363 27.7992 34.6569 27.1803 34.9981 26.6249C36.1883 24.7286 36.9104 22.4196 36.9104 19.9122C36.9104 17.397 36.1883 15.0881 34.9981 13.1917C34.6569 12.6362 34.7363 12.0174 35.2362 11.7159C35.7123 11.4302 36.3073 11.5651 36.6088 12.0571C38.0133 14.2866 38.8702 16.9765 38.8702 19.9122C38.8702 22.8401 38.0291 25.5379 36.6088 27.7675C36.3073 28.2515 35.7123 28.3864 35.2362 28.1007Z";

    private static final String ICON_LOSSLESS = "M16.0117 1.81812C18.4258 1.81812 19.8776 4.78099 20.958 7.92092L21.0631 8.23024L21.1659 8.54011L21.2666 8.84995C21.5987 9.88202 21.8972 10.9037 22.1828 11.8167C22.839 8.96008 22.4949 8.34671 23.1979 8.34671C23.4594 8.34671 23.724 8.53256 23.724 8.87123C23.724 9.0083 23.5105 10.461 23.2136 11.9166L23.1588 12.1809L23.1023 12.4436C23.0068 12.8791 22.9051 13.3002 22.8009 13.6704C25.8224 21.9469 28.0659 11.0453 28.3194 8.81691C28.3564 8.49556 28.5879 8.34672 28.8219 8.34672C29.1344 8.34672 29.3907 8.59283 29.3418 8.96318C28.8009 12.5671 27.9689 18.1818 24.5482 18.1818C22.6604 18.1818 21.5992 16.6374 20.7611 14.8678C20.179 13.6722 19.6841 12.2511 19.2288 10.8221L19.1203 10.4791C19.0844 10.3648 19.0487 10.2505 19.0132 10.1364L18.9073 9.79479C17.8713 6.44275 16.9926 3.33702 15.6457 3.33702C14.9875 3.33702 14.4995 4.05555 14.4657 4.05555C14.4036 4.05555 14.3548 3.76279 13.7401 2.96069C14.3251 2.26231 15.1314 1.81812 16.0117 1.81812ZM4.80934 1.82692C10.1957 1.82692 10.6747 16.6849 13.7232 16.6849C14.0811 16.6849 14.4669 16.4581 14.8886 15.9443C15.122 16.3577 15.3613 16.7247 15.6089 17.0478C15.0004 17.774 14.2433 18.1812 13.2952 18.1812C9.88906 18.1807 8.42559 12.2177 7.16605 8.19178C6.86941 9.48319 6.71544 10.6519 6.65516 11.1766C6.61775 11.5101 6.38397 11.662 6.14846 11.662C5.88728 11.662 5.62398 11.4752 5.62398 11.1416C5.62398 11.1167 5.62545 11.091 5.62848 11.0646C5.80292 9.74118 6.15443 7.73614 6.54802 6.3382C6.02411 4.90309 5.36971 3.33458 4.40561 3.33458C2.45924 3.33458 1.34461 8.44844 1.03117 11.1766C0.993764 11.5101 0.759979 11.662 0.524468 11.662C0.263303 11.662 0 11.4752 0 11.1416C0 11.1167 0.00146422 11.091 0.00449843 11.0646C0.0496669 10.7219 0.0980906 10.373 0.15073 10.0213L0.196904 9.71916C0.204779 9.66871 0.212746 9.61823 0.220807 9.56772L0.27033 9.26437C0.887195 5.57133 2.03712 1.82692 4.80934 1.82692ZM10.3981 1.81878C11.3654 1.81878 12.4031 2.34503 13.2976 3.57969C13.3476 3.63964 13.849 4.45427 14.0072 4.73669C14.554 5.77231 15.0302 7.038 15.4694 8.354L15.5832 8.69833C16.8447 12.5534 17.8174 16.7028 19.3326 16.7028C19.6918 16.7028 20.0814 16.4697 20.5126 15.9442C20.746 16.3575 20.9852 16.7245 21.2329 17.0476C20.6259 17.7724 19.8691 18.1812 18.9192 18.1812C13.565 18.1804 12.983 3.3313 10.0333 3.3313C9.36758 3.3313 8.87559 4.05559 8.84162 4.05559C8.7795 4.05559 8.7307 3.76287 8.11611 2.96083C8.7168 2.24348 9.52906 1.81878 10.3981 1.81878Z";

    private SkijaRenderer renderer;
    private SkijaRenderer bgRenderer;
    private AMLLFluidBackground.GuiRenderer gpuBackground;
    private SkijaRenderer staticRenderer;
    private SkijaRenderer controlsRenderer;
    private SkijaRenderer lyricsRenderer;
    private int guiScale = 1;
    private long openedAt;
    private long entryStartedAt;
    private long exitStartedAt;
    private long lastFrame;
    private long lastProgressRender;
    private long lastBgRender;
    private long lastDragRender;
    private int lastActiveLyric = -1;
    private int lastRenderedSecond = -1;
    private boolean dirty = true;
    private boolean controlsLayerDirty = true;
    private boolean lyricsLayerDirty = true;
    private float lyricScroll;
    private float lyricScrollVelocity;
    private float lyricRenderScroll;
    private float lyricLayerBlitOffsetY;
    private boolean lyricSnapOnNextRender = true;
    private float backgroundRenderTime;
    private float backgroundLowFreqPulse;
    private float backgroundPulseVisual;
    private int trackIndex;
    private Image coverImage;
    private List<LyricLine> lyricLines = List.of();
    private final List<CachedLyricLine> lyricCache = new ArrayList<>();
    private final Map<String, Path> iconPathCache = new HashMap<>();
    private float lyricCacheWidth = -1f;
    private float lyricCacheScale = -1f;
    private Typeface lyricCacheTypeface;
    private MusicLoader.MusicTrack cachedTrack;
    private MusicLoader.MusicTrack currentUiTrack;

    private boolean shuffleMode = false;
    private boolean repeatMode = false;
    private boolean playlistOpen = false;
    private float volume = 0.78f;
    private boolean draggingVolume = false;
    private boolean draggingProgress = false;
    private float progressDragSeconds = -1f;
    private boolean wasLeftDown = false;
    private boolean returningToMain = false;
    private boolean closingPage = false;
    private int hoveredControl = -1;

    private float volX, volY, volW, volH;
    private float progX, progY, progW, progH;
    private float prevX, prevY, nextX, nextY, playX, playY, btnR;
    private float modeX, modeY, repeatX, repeatY, listX, listY, listW, listH;
    private float controlsLayerX, controlsLayerY, controlsLayerW, controlsLayerH;
    private float lyricsLayerX, lyricsLayerY, lyricsLayerW, lyricsLayerH;
    private float layoutLeftX, layoutLeftW, layoutControlY;
    private float layoutRightX, layoutLyricTop, layoutRightW, layoutLyricH;

    private int paletteA = 0xFF9D5064;
    private int paletteB = 0xFF923823;
    private int paletteC = 0xFF604064;
    private int paletteDark = 0xFF24151B;

    public Musicpage() {
        super(Component.literal("Music"));
    }

    @Override
    protected void init() {
        super.init();
        guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        closeRenderer();
        int[] bgSize = computeBackgroundTextureSize();
        bgRenderer = new SkijaRenderer("music_page_bg_anim", bgSize[0], bgSize[1]);
        bgRenderer.clear(paletteDark);
        bgRenderer.upload();
        gpuBackground = new AMLLFluidBackground.GuiRenderer("music_page_bg_gpu");
        staticRenderer = new SkijaRenderer("music_page_static", Math.max(1, width * guiScale), Math.max(1, height * guiScale));
        controlsLayerX = 0f;
        controlsLayerY = 0f;
        controlsLayerW = Math.max(1f, width * 0.46f);
        controlsLayerH = Math.max(1f, height);
        lyricsLayerX = Math.max(0f, width * 0.46f);
        lyricsLayerY = 0f;
        lyricsLayerW = Math.max(1f, width - lyricsLayerX);
        lyricsLayerH = Math.max(1f, height);
        controlsRenderer = new SkijaRenderer("music_page_controls", Math.max(1, Math.round(controlsLayerW * guiScale)), Math.max(1, Math.round(controlsLayerH * guiScale)));
        lyricsRenderer = new SkijaRenderer("music_page_lyrics", Math.max(1, Math.round(lyricsLayerW * guiScale)), Math.max(1, Math.round(lyricsLayerH * guiScale)));
        renderer = staticRenderer;
        openedAt = System.currentTimeMillis();
        entryStartedAt = openedAt;
        exitStartedAt = 0L;
        returningToMain = false;
        closingPage = false;
        lastFrame = openedAt;
        lastProgressRender = 0;
        lastBgRender = 0;
        lastDragRender = 0;
        lyricRenderScroll = 0f;
        lyricLayerBlitOffsetY = 0f;
        lyricSnapOnNextRender = true;
        backgroundRenderTime = 0f;
        backgroundLowFreqPulse = 0f;
        backgroundPulseVisual = 0f;
        lastActiveLyric = -1;
        lastRenderedSecond = -1;
        dirty = true;
        controlsLayerDirty = true;
        lyricsLayerDirty = true;
        clampTrackIndex();
        refreshTrackCache();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (bgRenderer == null || staticRenderer == null || controlsRenderer == null || lyricsRenderer == null) return;
        try {
            long now = System.currentTimeMillis();
            if (returningToMain && now - exitStartedAt >= RETURN_ANIM_MS) {
                Minecraft.getInstance().gui.setScreen(new SkijaTestScreen());
                return;
            }
            if (closingPage && now - exitStartedAt >= CLOSE_ANIM_MS) {
                Minecraft.getInstance().gui.setScreen(null);
                return;
            }
            boolean exiting = isExiting();
            if (!exiting) handleMouse(mx, my);
            float dt = Math.min(0.05f, Math.max(0f, (now - lastFrame) / 1000f));
            lastFrame = now;
            if (dirty) {
                renderStaticPage(now);
                controlsLayerDirty = true;
                lyricsLayerDirty = true;
                dirty = false;
            }
            if (!exiting && currentUiTrack != null) {
                float currentSeconds = displayedElapsedSeconds(currentUiTrack, now);
                int currentSecond = (int) currentSeconds;
                int active = activeLyricIndex(currentSeconds);
                if (active != lastActiveLyric) {
                    if (!useLineLyricTextures()) lyricsLayerDirty = true;
                    lastActiveLyric = active;
                }
                updateLyricAnimation(active, pageScale(), dt);
                if (MusicLoader.isPlaying(currentUiTrack) && (currentSecond != lastRenderedSecond || now - lastProgressRender >= 250L)) {
                    controlsLayerDirty = true;
                    lastRenderedSecond = currentSecond;
                    lastProgressRender = now;
                }
            }
            if (!exiting) {
                if (controlsLayerDirty) {
                    renderControlsLayer(now);
                    controlsLayerDirty = false;
                }
                if (lyricsLayerDirty) {
                    if (!useLineLyricTextures()) renderLyricsLayer(now, dt);
                    lyricsLayerDirty = false;
                }
            }
            boolean bgAnimating = currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack) || entryProgress(now) < 1f;
            if (gpuBackground != null && gpuBackground.ready() && !exiting) {
                renderBackgroundLayer((now - openedAt) / 1000f);
                lastBgRender = now;
            } else {
                long bgInterval = exiting ? BG_INTERVAL_EXIT_MS : bgAnimating ? BG_INTERVAL_PLAYING_MS : BG_INTERVAL_IDLE_MS;
                if (lastBgRender == 0L || now - lastBgRender >= bgInterval) {
                    renderBackgroundLayer((now - openedAt) / 1000f);
                    lastBgRender = now;
                }
            }
            blitLayers(g);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.error("[Musicpage] Render error", e);
        }
        super.extractRenderState(g, mx, my, delta);
    }

    private void renderBackgroundLayer(float time) {
        if (bgRenderer == null) return;
        backgroundRenderTime = time;
        float targetPulse = currentUiTrack != null && MusicLoader.isPlaying(currentUiTrack)
                ? 0.035f + 0.018f * (0.5f + 0.5f * sin(time * 2.2f))
                : 0f;
        backgroundPulseVisual += (targetPulse - backgroundPulseVisual) * 0.06f;
        backgroundLowFreqPulse = backgroundPulseVisual;
        if (gpuBackground != null && gpuBackground.ready()) return;
        renderer = bgRenderer;
        int bw = bgRenderer.getWidth();
        int bh = bgRenderer.getHeight();
        renderer.clear(paletteDark);
        drawAnimatedFluidLayer(time, bw, bh);
        renderer.drawRoundedRect(0, 0, bw, bh, 0, 0x26000000);
        renderer.upload();
    }



    private void renderStaticPage(long now) {
        renderer = staticRenderer;
        renderer.clear(0x00000000);
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);

        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();

        if (!tracks.isEmpty()) {
            clampTrackIndex();
            MusicLoader.MusicTrack track = tracks.get(trackIndex);
            if (track != cachedTrack) {
                cachedTrack = track;
                lyricLines = parseLyrics(track.lyrics(), track.title(), track.artist());
                clearLyricCache();
                lyricScroll = 0f;
                lyricScrollVelocity = 0f;
                lyricRenderScroll = 0f;
                lyricLayerBlitOffsetY = 0f;
                lyricSnapOnNextRender = true;
                openedAt = now;
                rebuildCover(track);
            }
        }

        if (tracks.isEmpty()) {
            currentUiTrack = null;
            renderEmpty(tf);
        } else {
            renderAMLLStatic(tracks.get(trackIndex), tf, now);
        }

        c.restore();
        renderer.upload();
    }

    private void renderControlsLayer(long now) {
        if (controlsRenderer == null) return;
        renderer = controlsRenderer;
        renderer.clear(0x00000000);
        if (currentUiTrack == null) {
            renderer.upload();
            return;
        }
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        c.translate(-controlsLayerX, -controlsLayerY);
        SkijaTestScreen.ensureFontLoaded();
        renderControls(layoutLeftX, layoutControlY, layoutLeftW, pageScale(), SkijaTestScreen.curTf);
        renderProgress(layoutLeftX, layoutLeftW, currentUiTrack, SkijaTestScreen.curTf, now, pageScale());
        c.restore();
        renderer.upload();
    }

    private void renderLyricsLayer(long now, float dt) {
        if (lyricsRenderer == null) return;
        renderer = lyricsRenderer;
        renderer.clear(0x00000000);
        if (currentUiTrack == null) {
            renderer.upload();
            return;
        }
        Canvas c = renderer.canvas();
        c.save();
        c.scale(guiScale, guiScale);
        c.translate(-lyricsLayerX, -lyricsLayerY);
        SkijaTestScreen.ensureFontLoaded();
        int active = activeLyricIndex(elapsedSeconds(currentUiTrack, now));
        float scrollBase = lyricScroll;
        renderLyrics(layoutRightX, layoutLyricTop, layoutRightW, layoutLyricH, SkijaTestScreen.curTf, active, scrollBase, pageScale());
        lyricRenderScroll = scrollBase;
        lyricLayerBlitOffsetY = 0f;
        c.restore();
        renderer.upload();
    }



    private void drawAnimatedFluidLayer(float time, float w, float h) {
        float r1 = Math.max(w, h) * 0.38f;
        float r2 = Math.max(w, h) * 0.30f;
        float r3 = Math.max(w, h) * 0.24f;

        drawBlob(w * (0.22f + 0.055f * sin(time * 0.72f)), h * (0.28f + 0.075f * cos(time * 0.61f)), r1, withAlpha(paletteA, 0.18f));
        drawBlob(w * (0.72f + 0.070f * cos(time * 0.53f)), h * (0.35f + 0.060f * sin(time * 0.80f)), r2, withAlpha(paletteB, 0.15f));
        drawBlob(w * (0.54f + 0.090f * sin(time * 0.44f + 1.7f)), h * (0.76f + 0.055f * cos(time * 0.67f)), r3, withAlpha(paletteC, 0.16f));
    }

    private void drawBlob(float cx, float cy, float radius, int color) {
        renderer.drawCircle(cx, cy, radius, color);
        renderer.drawCircle(cx - radius * 0.18f, cy + radius * 0.10f, radius * 0.74f, withAlpha(color, alphaOf(color) * 0.56f));
        renderer.drawCircle(cx + radius * 0.26f, cy - radius * 0.16f, radius * 0.58f, withAlpha(color, alphaOf(color) * 0.42f));
    }

    private void renderEmpty(Typeface tf) {
        float s = pageScale();
        float cx = width / 2f;
        float cy = height / 2f;
        renderer.drawTextCentered("Music", cx, cy - 48 * s, tf, 38 * s, WHITE);
        renderer.drawTextCentered("没音乐你听个集冒", cx, cy - 8 * s, tf, 17 * s, W88);
        renderer.drawTextCentered("把 .mp3 / .flac 放到 lazychara/music，然后按 R 重新扫描", cx, cy + 22 * s, tf, 12 * s, W70);
        renderer.drawTextCentered("向上滚动返回 · RShift 关闭", cx, height - 26 * s, tf, 10 * s, W46);
    }

    private void renderAMLLStatic(MusicLoader.MusicTrack track, Typeface tf, long now) {
        float s = pageScale();
        float leftX = Math.max(22f * s, width * 0.075f);
        float leftW = Math.min(width * 0.36f, 430f * s);
        float coverSize = Math.min(leftW, height * 0.35f);
        coverSize = Math.max(82f * s, coverSize);
        float coverY = Math.max(22f * s, height * 0.070f);
        float infoY = coverY + coverSize + 18f * s;
        float controlY = 0f;

        float rightX = Math.max(width * 0.48f, leftX + leftW + 54f * s);
        float rightW = Math.max(140f * s, width - rightX - 42f * s);
        float lyricTop = Math.max(20f * s, height * 0.075f);
        float lyricH = height - lyricTop - 28f * s;

        currentUiTrack = track;
        layoutLeftX = leftX;
        layoutLeftW = leftW;
        layoutRightX = rightX;
        layoutRightW = rightW;
        layoutLyricTop = lyricTop;
        layoutLyricH = lyricH;

        float coverX = leftX + (leftW - coverSize) * 0.5f;
        renderCover(coverX, coverY, coverSize, s);
        float titleSize = fitTextSize(track.title(), tf, 16f * s, 8.5f * s, leftW);
        renderer.drawText(track.title(), leftX, infoY, tf, titleSize, WHITE);
        renderer.drawText(trimText(track.artist(), tf, 12f * s, leftW - 38f * s), leftX, infoY + 22f * s, tf, 12f * s, W70);

        float barY = infoY + 42f * s;

        float badgeH = 22f * s;
        String qualityLabel = track.qualityLabel();
        float badgeW = qualityLabel == null || qualityLabel.isBlank() ? 0f : losslessBadgeWidth(qualityLabel, tf, s);
        float badgeX = leftX + (leftW - badgeW) * 0.5f;
        float badgeY = barY + 10f * s;
        controlY = barY + 34f * s + badgeH + 42f * s;
        float maxControlY = height - 86f * s;
        if (controlY > maxControlY) {
            controlY = maxControlY;
        }
        layoutControlY = controlY;
        if (qualityLabel != null && !qualityLabel.isBlank()) {
            renderLosslessBadge(badgeX, badgeY, badgeW, badgeH, s, tf, qualityLabel);
        }
        renderPlaylistButton(width - 54f * s, height - 48f * s, 34f * s, s, tf);
        if (playlistOpen) renderPlaylistPanel(tf, s);
        updateLyricsLayerBounds(s);
    }

    private void updateLyricsLayerBounds(float s) {
        float padX = 36f * s;
        float padY = 88f * s;
        float x = clamp(layoutRightX - padX, 0f, width);
        float y = clamp(layoutLyricTop - padY, 0f, height);
        float w = clamp(layoutRightW + padX * 2f, 1f, width - x);
        float h = clamp(layoutLyricH + padY * 2f, 1f, height - y);
        int texW = Math.max(1, Math.round(w * guiScale));
        int texH = Math.max(1, Math.round(h * guiScale));
        boolean changed = lyricsRenderer == null || lyricsRenderer.getWidth() != texW || lyricsRenderer.getHeight() != texH || Math.abs(lyricsLayerX - x) > 0.5f || Math.abs(lyricsLayerY - y) > 0.5f;
        lyricsLayerX = x;
        lyricsLayerY = y;
        lyricsLayerW = w;
        lyricsLayerH = h;
        if (lyricsRenderer == null || lyricsRenderer.getWidth() != texW || lyricsRenderer.getHeight() != texH) {
            if (lyricsRenderer != null) lyricsRenderer.close();
            lyricsRenderer = new SkijaRenderer("music_page_lyrics", texW, texH);
        }
        if (changed) lyricsLayerDirty = true;
    }

    private void renderProgress(float leftX, float leftW, MusicLoader.MusicTrack track, Typeface tf, long now, float s) {
        float coverSize = Math.min(leftW, height * 0.35f);
        coverSize = Math.max(82f * s, coverSize);
        float coverY = Math.max(22f * s, height * 0.070f);
        float infoY = coverY + coverSize + 18f * s;
        float elapsed = displayedElapsedSeconds(track, now);
        float duration = Math.max(1, track.duration());
        float progress = clamp(elapsed / duration, 0f, 1f);
        float barY = infoY + 42f * s;
        renderer.drawRoundedRect(leftX, barY, leftW, 5f * s, 2.5f * s, W30);
        if (progress > 0f) renderer.drawRoundedRect(leftX, barY, Math.max(5f * s, leftW * progress), 5f * s, 2.5f * s, W88);
        renderer.drawText(formatTime((int) elapsed), leftX, barY + 14f * s, tf, 8.5f * s, W70);
        String remain = "-" + formatTime(Math.max(0, track.duration() - (int) elapsed));
        float remainW = renderer.measureText(remain, tf, 8.5f * s);
        renderer.drawText(remain, leftX + leftW - remainW, barY + 14f * s, tf, 8.5f * s, W70);
        progX = leftX;
        progY = barY;
        progW = leftW;
        progH = 16f * s;
    }

    private void renderCover(float x, float y, float size, float s) {
        renderer.drawSquircle(x, y + 8f * s, size, size, 6f * s, 0x24000000);
        renderer.drawSquircle(x, y, size, size, 6f * s, W12);
        if (coverImage != null) {
            renderer.canvas().save();
            renderer.canvas().clipRRect(io.github.humbleui.types.RRect.makeXYWH(x, y, size, size, 6f * s));
            renderer.canvas().drawImageRect(coverImage, Rect.makeXYWH(x, y, size, size));
            renderer.canvas().restore();
        } else {
            renderer.drawCircle(x + size / 2f, y + size / 2f, size * 0.32f, W18);
            renderer.drawCircle(x + size / 2f, y + size / 2f, size * 0.12f, W30);
        }
    }

    private float losslessBadgeWidth(String label, Typeface tf, float s) {
        float textSize = 10.5f * s;
        float iconW = 18f * s;
        float gap = 6f * s;
        float padX = 9f * s;
        return renderer.measureText(label, tf, textSize) + iconW + gap + padX * 2f;
    }

    private void renderLosslessBadge(float x, float y, float w, float h, float s, Typeface tf, String label) {
        renderer.drawRoundedRect(x, y, w, h, 7f * s, 0x24FFFFFF);
        float textSize = 10.5f * s;
        float iconW = 18f * s;
        float iconCx = x + 9f * s + iconW * 0.5f;
        float iconCy = y + h * 0.5f;
        drawIconRect(new String[]{ICON_LOSSLESS}, 30f, 20f, iconCx, iconCy, iconW, W88);
        renderer.drawText(label, x + 9f * s + iconW + 6f * s, y + (h - textSize) * 0.5f - 0.5f * s, tf, textSize, W88);
    }

    private void renderControls(float x, float y, float w, float s, Typeface tf) {
        float buttonGap = w * 0.205f;
        float hoverR = clamp(buttonGap * 0.43f, 24f * s, 36f * s);
        float toggleSize = clamp(buttonGap * 0.46f, 30f * s, 40f * s);
        float transportSize = clamp(buttonGap * 0.66f, 42f * s, 56f * s);
        float playSize = clamp(buttonGap * 0.56f, 40f * s, 52f * s);
        btnR = hoverR;
        float buttonsY = y + 16f * s;
        modeX = x + w * 0.09f;
        modeY = buttonsY;
        prevX = x + w * 0.295f;
        prevY = buttonsY;
        playX = x + w * 0.50f;
        playY = buttonsY;
        nextX = x + w * 0.705f;
        nextY = buttonsY;
        repeatX = x + w * 0.91f;
        repeatY = buttonsY;

        drawShuffleButton(modeX, modeY, toggleSize, shuffleMode, hoveredControl == 0, hoverR);
        drawPrevNext(prevX, prevY, transportSize, false, hoveredControl == 1, hoverR);
        drawPlayPause(playX, playY, playSize, !MusicLoader.isPlaying(currentUiTrack), hoveredControl == 2, hoverR);
        drawPrevNext(nextX, nextY, transportSize, true, hoveredControl == 3, hoverR);
        drawRepeatButton(repeatX, repeatY, toggleSize, repeatMode, hoveredControl == 4, hoverR);

        volume = MusicLoader.getVolume();
        volX = x + w * 0.17f;
        volY = y + 58f * s;
        volW = w * 0.66f;
        volH = 8f * s;
        float volCy = volY + volH * 0.5f;
        drawSpeaker(x + w * 0.08f, volCy, 13.5f * s, AMLL_VOLUME);
        renderer.drawRoundedRect(volX, volY, volW, volH, volH / 2f, AMLL_VOLUME_BG);
        if (volume > 0f) renderer.drawRoundedRect(volX, volY, Math.max(volH, volW * volume), volH, volH / 2f, AMLL_VOLUME_FILL);
        drawSpeakerWithWaves(x + w * 0.92f, volCy, 13.5f * s, AMLL_VOLUME);
    }

    private void renderPlaylistPanel(Typeface tf, float s) {
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();
        float w = Math.min(300f * s, width * 0.30f);
        float h = Math.min(260f * s, height * 0.48f);
        float x = width - w - 26f * s;
        float y = height - h - 88f * s;
        renderer.drawRoundedRect(x, y, w, h, 14f * s, 0x3AFFFFFF);
        renderer.drawRoundedRectStroke(x, y, w, h, 14f * s, 1.0f * s, 0x55FFFFFF);
        renderer.drawText("播放列表", x + 14f * s, y + 10f * s, tf, 12f * s, WHITE);
        int maxRows = Math.max(1, (int) ((h - 42f * s) / (26f * s)));
        int rows = Math.min(maxRows, tracks.size());
        for (int i = 0; i < rows; i++) {
            MusicLoader.MusicTrack track = tracks.get(i);
            float rowY = y + 34f * s + i * 26f * s;
            if (i == trackIndex) renderer.drawRoundedRect(x + 8f * s, rowY - 2f * s, w - 16f * s, 22f * s, 7f * s, 0x2AFFFFFF);
            String title = trimText(track.title(), tf, 10.5f * s, w - 28f * s);
            renderer.drawText(title, x + 14f * s, rowY, tf, 10.5f * s, i == trackIndex ? WHITE : W70);
        }
    }

    private void renderPlaylistButton(float x, float y, float size, float s, Typeface tf) {
        listX = x;
        listY = y;
        listW = size;
        listH = size;
        renderer.drawRoundedRect(x, y, size, size, 8f * s, W18);
        float lx = x + 9f * s;
        float ly = y + 10f * s;
        for (int i = 0; i < 3; i++) {
            renderer.drawCircle(lx, ly + i * 7f * s, 1.4f * s, W88);
            renderer.drawRoundedRect(lx + 5f * s, ly + i * 7f * s - 1f * s, 13f * s, 2f * s, 1f * s, W88);
        }
    }

    private void renderLyrics(float x, float y, float w, float h, Typeface tf, int active, float scrollBase, float s) {
        float activePreferred = Math.max(22f, 28f * s);
        float inactivePreferred = Math.max(17f, 24f * s);
        ensureLyricCache(tf, w, activePreferred, inactivePreferred, s);

        float gap = 14f * s;
        float cumY = 0f;
        int count = lyricLines.size();
        float[] offsets = new float[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = cumY;
            cumY += getLineContentHeight(i, active, s) + gap;
        }

        float anchorY = y + h * 0.35f;

        Canvas c = renderer.canvas();
        c.save();
        c.clipRect(Rect.makeXYWH(x - 18f * s, lyricsLayerY, w + 36f * s, lyricsLayerH));
        float baseY = anchorY - scrollBase;
        for (int i = 0; i < count; i++) {
            float lineY = baseY + offsets[i];
            CachedLyricLine cached = i < lyricCache.size() ? lyricCache.get(i) : null;
            if (cached == null) continue;

            boolean isActive = i == active;
            float lineH = getLineContentHeight(i, active, s);

            if (lineY + lineH + 30f * s < lyricsLayerY || lineY - 30f * s > lyricsLayerY + lyricsLayerH) continue;

            float textX = x;
            if (isActive) {
                renderer.canvas().drawImageRect(cached.activeImage, Rect.makeXYWH(textX - cached.activePad, lineY - cached.activePad, cached.activeW, cached.activeH));
            } else {
                renderer.canvas().drawImageRect(cached.inactiveImage, Rect.makeXYWH(textX - cached.inactivePad, lineY - cached.inactivePad, cached.inactiveW, cached.inactiveH));
            }
        }
        c.restore();
    }

    private float getLineContentHeight(int index, int activeIndex, float s) {
        CachedLyricLine cached = index >= 0 && index < lyricCache.size() ? lyricCache.get(index) : null;
        if (cached == null) return 30f * s;
        if (index == activeIndex) {
            return Math.max(1f, cached.activeH - cached.activePad * 2f);
        } else {
            return Math.max(1f, cached.inactiveH - cached.inactivePad * 2f);
        }
    }

    private boolean ensureLyricCache(Typeface tf, float maxWidth, float activePreferred, float inactivePreferred, float s) {
        if (tf == null) return false;
        if (lyricCache.size() == lyricLines.size() && Math.abs(lyricCacheWidth - maxWidth) < 0.5f && Math.abs(lyricCacheScale - s) < 0.001f && lyricCacheTypeface == tf) return false;
        clearLyricCache();
        lyricCacheWidth = maxWidth;
        lyricCacheScale = s;
        lyricCacheTypeface = tf;
        for (LyricLine line : lyricLines) {
            String text = line.text();
            float activeSize = fitLyricTextSize(text, tf, activePreferred, Math.max(16f, 20f * s), maxWidth);
            float inactiveSize = activeSize;
            TextImage active = createLyricTextImage(text, tf, activeSize, WHITE, 3.2f * s, false, maxWidth);
            TextImage inactive = createLyricTextImage(text, tf, inactiveSize, WHITE, 6.0f * s, true, maxWidth);
            lyricCache.add(new CachedLyricLine(active, inactive));
        }
        return true;
    }

    private TextImage createLyricTextImage(String text, Typeface tf, float size, int color, float blurSigma, boolean drawFaintCore, float maxWidth) {
        if (text == null) text = "";
        String[] lines = wrapText(text, tf, size, maxWidth);
        float scale = Math.max(1f, guiScale);
        try (Font font = new Font(tf, size * scale)) {
            float lineH = Math.max(1f, (font.getMetrics().getDescent() - font.getMetrics().getAscent()) / scale * 1.12f);
            float maxLineW = 1f;
            for (String line : lines) maxLineW = Math.max(maxLineW, renderer.measureText(line, tf, size));
            float pad = blurSigma > 0f ? Math.max(10f, blurSigma * (drawFaintCore ? 3.0f : 5.5f)) : 0f;
            float logicalW = maxLineW + pad * 2f + 4f;
            float logicalH = lineH * lines.length + pad * 2f + 4f;
            int imgW = Math.max(1, Math.round(logicalW * scale));
            int imgH = Math.max(1, Math.round(logicalH * scale));
            SkijaRenderer textRenderer = new SkijaRenderer("music_page_lyric_line", imgW, imgH);
            textRenderer.clear(0x00000000);
            Canvas sc = textRenderer.canvas();
            sc.save();
            sc.scale(scale, scale);
            try (Font logicalFont = new Font(tf, size); Paint paint = new Paint()) {
                paint.setColor(color);
                paint.setAntiAlias(true);
                if (blurSigma > 0f) {
                    if (!drawFaintCore) {
                        try (MaskFilter outerBlur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma * 2.15f, false)) {
                            paint.setMaskFilter(outerBlur);
                            paint.setColor(0x34FFFFFF);
                            drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                        }
                    }
                    try (MaskFilter blur = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurSigma, false)) {
                        paint.setMaskFilter(blur);
                        paint.setColor(drawFaintCore ? color : 0xA6FFFFFF);
                        drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                    }
                    paint.setMaskFilter(null);
                    paint.setColor(drawFaintCore ? 0x70FFFFFF : color);
                    drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                } else {
                    drawTextLines(sc, lines, pad + 2f, pad + 2f, logicalFont, paint, lineH);
                }
            }
            sc.restore();
            textRenderer.upload();
            Image image = textRenderer.getSurface().makeImageSnapshot();
            return new TextImage(image, textRenderer, logicalW, logicalH, pad + 2f);
        }
    }

    private void drawTextLines(Canvas c, String[] lines, float x, float y, Font font, Paint paint, float lineH) {
        float ascent = font.getMetrics().getAscent();
        for (int i = 0; i < lines.length; i++) {
            c.drawString(lines[i], x, y + i * lineH - ascent, font, paint);
        }
    }

    private String[] wrapText(String text, Typeface tf, float size, float maxWidth) {
        String normalized = normalizeLyricText(text);
        if (normalized.isEmpty()) return new String[]{""};
        if (renderer.measureText(normalized, tf, size) <= maxWidth) return new String[]{normalized};
        List<TextSegment> segments = lyricTextSegments(normalized, tf, size);
        List<Integer> breaks = balancedLyricBreaks(segments, maxWidth);
        if (breaks.isEmpty()) return new String[]{normalized};
        Set<Integer> breakSet = new HashSet<>(breaks);
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (breakSet.contains(i) && !cur.isEmpty()) {
                String line = trimLyricDrawLine(cur.toString());
                if (!line.isEmpty()) lines.add(line);
                cur.setLength(0);
            }
            cur.append(segments.get(i).text());
        }
        String last = trimLyricDrawLine(cur.toString());
        if (!last.isEmpty()) lines.add(last);
        return lines.isEmpty() ? new String[]{normalized} : lines.toArray(String[]::new);
    }

    private float fitLyricTextSize(String text, Typeface tf, float preferredSize, float minSize, float maxWidth) {
        String normalized = normalizeLyricText(text);
        float size = preferredSize;
        while (size > minSize && renderer.measureText(normalized, tf, size) > maxWidth) {
            size -= 1.4f;
        }
        while (size > minSize && longestLyricSegmentWidth(normalized, tf, size) > maxWidth) {
            size -= 1.4f;
        }
        return Math.max(minSize, size);
    }

    private float longestLyricSegmentWidth(String text, Typeface tf, float size) {
        String normalized = normalizeLyricText(text);
        if (normalized.isEmpty()) return 0f;
        float result = 0f;
        for (TextSegment segment : lyricTextSegments(normalized, tf, size)) {
            if (!segment.isSpace()) result = Math.max(result, segment.width());
        }
        return result;
    }

    private String normalizeLyricText(String text) {
        if (text == null) return "";
        return LYRIC_SPACE_PATTERN.matcher(text.strip()).replaceAll(" ");
    }

    private String trimLyricDrawLine(String text) {
        if (text == null) return "";
        return text.strip();
    }

    private List<TextSegment> lyricTextSegments(String text, Typeface tf, float size) {
        ArrayList<TextSegment> result = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String part = text.substring(start, end);
            if (part.isEmpty()) continue;
            appendLyricTextSegment(result, part, tf, size);
        }
        if (result.isEmpty()) result.add(new TextSegment(text, renderer.measureText(text, tf, size), text.isBlank()));
        return result;
    }

    private void appendLyricTextSegment(List<TextSegment> result, String part, Typeface tf, float size) {
        if (part.isBlank()) {
            result.add(new TextSegment(part, renderer.measureText(part, tf, size), true));
            return;
        }
        if (!containsCjk(part)) {
            result.add(new TextSegment(part, renderer.measureText(part, tf, size), false));
            return;
        }
        StringBuilder pending = new StringBuilder();
        for (int offset = 0; offset < part.length();) {
            int cp = part.codePointAt(offset);
            int len = Character.charCount(cp);
            String unit = part.substring(offset, offset + len);
            if (isCjkCodePoint(cp)) {
                if (!pending.isEmpty()) {
                    String pendingText = pending.toString();
                    result.add(new TextSegment(pendingText, renderer.measureText(pendingText, tf, size), false));
                    pending.setLength(0);
                }
                result.add(new TextSegment(unit, renderer.measureText(unit, tf, size), false));
            } else {
                pending.append(unit);
            }
            offset += len;
        }
        if (!pending.isEmpty()) {
            String pendingText = pending.toString();
            result.add(new TextSegment(pendingText, renderer.measureText(pendingText, tf, size), false));
        }
    }

    private List<Integer> balancedLyricBreaks(List<TextSegment> segments, float maxWidth) {
        int n = segments.size();
        ArrayList<Integer> result = new ArrayList<>();
        if (n <= 1 || maxWidth <= 1f) return result;
        double[] prefixWidth = new double[n + 1];
        for (int i = 0; i < n; i++) prefixWidth[i + 1] = prefixWidth[i] + segments.get(i).width();
        if (prefixWidth[n] <= maxWidth) return result;
        double[] dp = new double[n + 1];
        int[] nextBreak = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            dp[i] = Double.POSITIVE_INFINITY;
            nextBreak[i] = -1;
        }
        dp[n] = 0.0;
        double cjkPenalty = Math.pow(maxWidth * LYRIC_CJK_BREAK_PENALTY_RATIO, 2.0);
        double normalPenalty = Math.pow(maxWidth * LYRIC_NORMAL_BREAK_PENALTY_RATIO, 2.0);
        for (int i = n - 1; i >= 0; i--) {
            for (int j = i + 1; j <= n; j++) {
                double lineW = prefixWidth[j] - prefixWidth[i];
                double lineCost;
                if (lineW > maxWidth) {
                    if (j == i + 1) lineCost = Math.pow(lineW - maxWidth, 2.0) * LYRIC_OVERFLOW_PENALTY_MULTIPLIER;
                    else continue;
                } else {
                    lineCost = Math.pow(maxWidth - lineW, 2.0);
                }
                double breakPenalty = 0.0;
                if (j < n) {
                    TextSegment prev = segments.get(j - 1);
                    if (endsWithLyricPunctuation(prev.text())) breakPenalty = -Math.pow(maxWidth * LYRIC_PUNCTUATION_BREAK_REWARD_RATIO, 2.0);
                    else if (prev.isSpace()) breakPenalty = -Math.pow(maxWidth * LYRIC_SPACE_BREAK_REWARD_RATIO, 2.0);
                    else if (isCjkBreakBoundary(segments, j)) breakPenalty = cjkPenalty;
                    else breakPenalty = normalPenalty;
                }
                double total = lineCost + breakPenalty + dp[j];
                if (total < dp[i]) {
                    dp[i] = total;
                    nextBreak[i] = j;
                }
            }
        }
        int cur = 0;
        while (cur < n) {
            int next = nextBreak[cur];
            if (next <= cur || next > n) break;
            if (next < n) result.add(next);
            cur = next;
        }
        return result;
    }

    private boolean endsWithLyricPunctuation(String text) {
        if (text == null || text.isEmpty()) return false;
        String stripped = text.stripTrailing();
        if (stripped.isEmpty()) return false;
        int cp = stripped.codePointBefore(stripped.length());
        return LYRIC_BREAK_PUNCTUATION.indexOf(cp) >= 0;
    }

    private boolean isCjkBreakBoundary(List<TextSegment> segments, int index) {
        if (index <= 0 || index >= segments.size()) return false;
        return containsCjk(segments.get(index - 1).text()) || containsCjk(segments.get(index).text());
    }

    private boolean containsCjk(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            if (isCjkCodePoint(cp)) return true;
            offset += Character.charCount(cp);
        }
        return false;
    }

    private boolean isCjkCodePoint(int cp) {
        Character.UnicodeScript script = Character.UnicodeScript.of(cp);
        return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL || script == Character.UnicodeScript.BOPOMOFO;
    }

    private void clearLyricCache() {
        for (CachedLyricLine line : lyricCache) line.close();
        lyricCache.clear();
        lyricCacheWidth = -1f;
        lyricCacheScale = -1f;
        lyricCacheTypeface = null;
    }

    private boolean useLineLyricTextures() {
        return currentUiTrack != null && !lyricLines.isEmpty();
    }

    private void updateLyricAnimation(int active, float s, float dt) {
        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        if (tf != null && layoutRightW > 1f) {
            float activePreferred = Math.max(22f, 28f * s);
            float inactivePreferred = Math.max(17f, 24f * s);
            if (ensureLyricCache(tf, layoutRightW, activePreferred, inactivePreferred, s)) lyricSnapOnNextRender = true;
        }
        boolean snap = lyricSnapOnNextRender;
        float targetScroll = lyricScrollTarget(active, s);
        if (snap) {
            lyricScroll = targetScroll;
            lyricScrollVelocity = 0f;
        } else {
            updateLyricSpring(active, targetScroll, dt);
        }
        lyricLayerBlitOffsetY = lyricRenderScroll - lyricScroll;
        if (!useLineLyricTextures()) {
            float safeOffset = Math.max(38f * s, Math.min(lyricsLayerH * 0.22f, 88f * s));
            if (Math.abs(lyricLayerBlitOffsetY) > safeOffset) lyricsLayerDirty = true;
        }
        updateLyricLineAnimations(active, s, dt, snap);
        lyricSnapOnNextRender = false;
    }

    private float lyricScrollTarget(int active, float s) {
        float gap = 14f * s;
        float target = 0f;
        int count = Math.min(Math.max(active, 0), lyricLines.size());
        for (int i = 0; i < count; i++) {
            target += getLineContentHeight(i, active, s) + gap;
        }
        return target;
    }

    private SpringParams lyricSpringParams(int active) {
        float stiffness = 90f;
        float damping = 15f;
        if (active > 0 && active < lyricLines.size()) {
            float intervalMs = (lyricLines.get(active).time() - lyricLines.get(active - 1).time()) * 1000f;
            float clampedInterval = clamp(intervalMs, 100f, 800f);
            float ratio = 1f - (clampedInterval - 100f) / 700f;
            ratio = (float) Math.pow(ratio, 0.2f);
            stiffness = 170f + ratio * 50f;
            damping = (float) Math.sqrt(stiffness) * 2.2f;
        }
        return new SpringParams(stiffness, damping);
    }

    private void updateLyricSpring(int active, float targetScroll, float dt) {
        float safeDt = clamp(dt, 0f, 0.05f);
        SpringParams params = lyricSpringParams(active);
        float displacement = targetScroll - lyricScroll;
        lyricScrollVelocity += displacement * params.stiffness() * safeDt;
        lyricScrollVelocity *= (float) Math.exp(-params.damping() * safeDt);
        lyricScroll += lyricScrollVelocity * safeDt;
        if (Math.abs(displacement) < 0.25f && Math.abs(lyricScrollVelocity) < 0.35f) {
            lyricScroll = targetScroll;
            lyricScrollVelocity = 0f;
        }
    }

    private void updateLyricLineAnimations(int active, float s, float dt, boolean snap) {
        if (lyricCache.size() != lyricLines.size()) return;
        float safeDt = clamp(dt, 0f, 0.05f);
        SpringParams params = lyricSpringParams(active);
        float gap = 14f * s;
        float anchorY = layoutLyricTop + layoutLyricH * 0.35f;
        float baseY = anchorY - lyricScroll;
        float offsetY = 0f;
        for (int i = 0; i < lyricCache.size(); i++) {
            CachedLyricLine line = lyricCache.get(i);
            LyricPresentation presentation = computeLyricPresentation(i, active);
            float targetY = baseY + offsetY;
            if (!line.initialized || snap) {
                line.currentY = targetY;
                line.velocityY = 0f;
                line.currentScale = presentation.scale();
                line.velocityScale = 0f;
                line.currentOpacity = presentation.opacity();
                line.velocityOpacity = 0f;
                line.initialized = true;
            } else {
                float dy = targetY - line.currentY;
                line.velocityY += dy * params.stiffness() * safeDt;
                line.velocityY *= (float) Math.exp(-params.damping() * safeDt);
                line.currentY += line.velocityY * safeDt;
                float scaleStiffness = params.stiffness() * 1.08f;
                float scaleDamping = (float) Math.sqrt(scaleStiffness) * 2.15f;
                float ds = presentation.scale() - line.currentScale;
                line.velocityScale += ds * scaleStiffness * safeDt;
                line.velocityScale *= (float) Math.exp(-scaleDamping * safeDt);
                line.currentScale += line.velocityScale * safeDt;
                float opacityStiffness = params.stiffness() * 1.35f;
                float opacityDamping = (float) Math.sqrt(opacityStiffness) * 2.1f;
                float da = presentation.opacity() - line.currentOpacity;
                line.velocityOpacity += da * opacityStiffness * safeDt;
                line.velocityOpacity *= (float) Math.exp(-opacityDamping * safeDt);
                line.currentOpacity += line.velocityOpacity * safeDt;
                if (Math.abs(dy) < 0.20f && Math.abs(line.velocityY) < 0.30f) {
                    line.currentY = targetY;
                    line.velocityY = 0f;
                }
                if (Math.abs(ds) < 0.001f && Math.abs(line.velocityScale) < 0.002f) {
                    line.currentScale = presentation.scale();
                    line.velocityScale = 0f;
                }
                if (Math.abs(da) < 0.004f && Math.abs(line.velocityOpacity) < 0.006f) {
                    line.currentOpacity = presentation.opacity();
                    line.velocityOpacity = 0f;
                }
            }
            offsetY += getLineContentHeight(i, active, s) + gap;
        }
    }

    private LyricPresentation computeLyricPresentation(int index, int active) {
        int dist = Math.abs(index - active);
        if (dist == 0) return new LyricPresentation(1f, 1f);
        float opacity;
        if (dist == 1) opacity = 0.58f;
        else if (dist == 2) opacity = 0.42f;
        else if (dist == 3) opacity = 0.31f;
        else opacity = 0.23f;
        if (index < active) opacity *= 0.78f;
        float scale = dist == 1 ? 0.975f : dist == 2 ? 0.955f : 0.94f;
        return new LyricPresentation(clamp(opacity, 0.18f, 0.62f), scale);
    }



    private void drawPlayPause(float cx, float cy, float size, boolean showPlay, boolean hover, float hoverR) {
        drawButtonHover(cx, cy, hoverR, hover);
        drawIcon(new String[]{showPlay ? ICON_PLAY : ICON_PAUSE}, 38f, cx, cy, size, WHITE);
    }

    private void drawPrevNext(float cx, float cy, float size, boolean next, boolean hover, float hoverR) {
        drawButtonHover(cx, cy, hoverR, hover);
        drawIcon(next ? new String[]{ICON_FORWARD_LEFT, ICON_FORWARD_RIGHT} : new String[]{ICON_REWIND_RIGHT, ICON_REWIND_LEFT}, 134f, cx, cy, size, WHITE);
    }

    private void drawShuffleButton(float cx, float cy, float size, boolean active, boolean hover, float hoverR) {
        drawToggleIcon(new String[]{ICON_SHUFFLE}, cx, cy, size, active, hover, hoverR);
    }

    private void drawRepeatButton(float cx, float cy, float size, boolean active, boolean hover, float hoverR) {
        drawToggleIcon(new String[]{ICON_REPEAT}, cx, cy, size, active, hover, hoverR);
    }

    private void drawToggleIcon(String[] paths, float cx, float cy, float size, boolean active, boolean hover, float hoverR) {
        if (active) {
            renderer.drawSquircle(cx - size * 0.5f, cy - size * 0.5f, size, size, size * 0.28f, AMLL_ACTIVE_BG);
            drawIcon(paths, 56f, cx, cy, size, AMLL_ACTIVE_ICON);
        } else {
            drawButtonHover(cx, cy, hoverR, hover);
            drawIcon(paths, 56f, cx, cy, size, WHITE);
        }
    }

    private void drawButtonHover(float cx, float cy, float radius, boolean hover) {
        if (hover) renderer.drawCircle(cx, cy, radius, AMLL_BUTTON_HOVER);
    }

    private void drawSpeaker(float cx, float cy, float r, int color) {
        drawIconRect(new String[]{ICON_SPEAKER}, 32f, 40f, cx, cy, r * 2.0f, color);
    }

    private void drawSpeakerWithWaves(float cx, float cy, float r, int color) {
        drawIconRect(new String[]{ICON_SPEAKER3_BODY, ICON_SPEAKER3_W1, ICON_SPEAKER3_W2, ICON_SPEAKER3_W3}, 43f, 40f, cx, cy, r * 2.0f, color);
    }

    private void drawIcon(String[] paths, float viewBox, float cx, float cy, float size, int color) {
        drawIconRect(paths, viewBox, viewBox, cx, cy, size, color);
    }

    private void drawIconRect(String[] paths, float viewBoxW, float viewBoxH, float cx, float cy, float size, int color) {
        Canvas c = renderer.canvas();
        c.save();
        float scale = size / Math.max(viewBoxW, viewBoxH);
        float drawW = viewBoxW * scale;
        float drawH = viewBoxH * scale;
        c.translate(cx - drawW * 0.5f, cy - drawH * 0.5f);
        c.scale(scale, scale);
        try (Paint paint = new Paint()) {
            paint.setColor(color);
            paint.setAntiAlias(true);
            for (String data : paths) {
                Path path = iconPath(data);
                if (path != null) c.drawPath(path, paint);
            }
        }
        c.restore();
    }

    private Path iconPath(String data) {
        if (data == null || data.isBlank()) return null;
        Path cached = iconPathCache.get(data);
        if (cached != null) return cached;
        Path path = Path.makeFromSVGString(data);
        if (path != null) iconPathCache.put(data, path);
        return path;
    }


    private void handleMouse(int mx, int my) {
        long window = Minecraft.getInstance().getWindow().handle();
        long now = System.currentTimeMillis();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean leftClick = leftDown && !wasLeftDown;
        boolean wasDraggingProgress = draggingProgress;
        boolean wasDraggingVolume = draggingVolume;
        if (leftClick) handleControlClick(mx, my);
        if (!leftDown) {
            if (wasDraggingProgress) finishProgressDrag();
            else progressDragSeconds = -1f;
            draggingVolume = false;
            if (wasDraggingVolume) controlsLayerDirty = true;
        }
        if (draggingProgress && currentUiTrack != null && progW > 0) {
            float p = clamp((mx - progX) / progW, 0f, 1f);
            progressDragSeconds = currentUiTrack.duration() * p;
            if (now - lastDragRender >= DRAG_RENDER_INTERVAL_MS) {
                controlsLayerDirty = true;
                lyricsLayerDirty = true;
                lastDragRender = now;
            }
        }
        if (draggingVolume && volW > 0) {
            volume = clamp((mx - volX) / volW, 0f, 1f);
            MusicLoader.setVolume(volume);
            if (now - lastDragRender >= DRAG_RENDER_INTERVAL_MS) {
                controlsLayerDirty = true;
                lastDragRender = now;
            }
        }
        updateControlHover(mx, my);
        wasLeftDown = leftDown;
    }

    private void updateControlHover(float mx, float my) {
        int hover = -1;
        if (inside(mx, my, modeX - btnR, modeY - btnR, btnR * 2f, btnR * 2f)) hover = 0;
        else if (inside(mx, my, prevX - btnR, prevY - btnR, btnR * 2f, btnR * 2f)) hover = 1;
        else if (inside(mx, my, playX - btnR, playY - btnR, btnR * 2f, btnR * 2f)) hover = 2;
        else if (inside(mx, my, nextX - btnR, nextY - btnR, btnR * 2f, btnR * 2f)) hover = 3;
        else if (inside(mx, my, repeatX - btnR, repeatY - btnR, btnR * 2f, btnR * 2f)) hover = 4;
        if (hoveredControl != hover) {
            hoveredControl = hover;
            controlsLayerDirty = true;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleControlClick((float) event.x(), (float) event.y())) {
            wasLeftDown = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingProgress) {
            if (currentUiTrack != null && progW > 0f) progressDragSeconds = currentUiTrack.duration() * clamp(((float) event.x() - progX) / progW, 0f, 1f);
            finishProgressDrag();
            wasLeftDown = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean handleControlClick(float mx, float my) {
        if (playlistOpen) {
            if (playlistContains(mx, my)) {
                int row = playlistRowAt(mx, my);
                if (row >= 0 && row < MusicLoader.getTracks().size()) {
                    trackIndex = row;
                    refreshTrackCache();
                    playSelectedTrackFromStart();
                    playlistOpen = false;
                    dirty = true;
                    return true;
                }
            } else if (!inside(mx, my, listX, listY, listW, listH)) {
                playlistOpen = false;
                dirty = true;
                return true;
            }
        }
        if (inside(mx, my, playX - btnR, playY - btnR, btnR * 2f, btnR * 2f)) {
            if (currentUiTrack != null) {
                MusicLoader.toggle(currentUiTrack);
                if (MusicLoader.isPlaying(currentUiTrack)) resetPlaybackUiState();
            }
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, prevX - btnR, prevY - btnR, btnR * 2f, btnR * 2f)) {
            previousTrack();
            return true;
        }
        if (inside(mx, my, nextX - btnR, nextY - btnR, btnR * 2f, btnR * 2f)) {
            nextTrack();
            return true;
        }
        if (inside(mx, my, modeX - btnR, modeY - btnR, btnR * 2f, btnR * 2f)) {
            shuffleMode = !shuffleMode;
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, repeatX - btnR, repeatY - btnR, btnR * 2f, btnR * 2f)) {
            repeatMode = !repeatMode;
            controlsLayerDirty = true;
            return true;
        }
        if (inside(mx, my, listX, listY, listW, listH)) {
            playlistOpen = !playlistOpen;
            dirty = true;
            return true;
        }
        if (inside(mx, my, progX, progY - 8, progW, progH + 12)) {
            draggingProgress = true;
            progressDragSeconds = currentUiTrack != null ? currentUiTrack.duration() * clamp((mx - progX) / progW, 0f, 1f) : -1f;
            if (currentUiTrack != null && progressDragSeconds >= 0f) playDisplayedTrackFrom(progressDragSeconds);
            resetPlaybackUiState();
            controlsLayerDirty = true;
            lyricsLayerDirty = true;
            lastDragRender = System.currentTimeMillis();
            return true;
        }
        if (inside(mx, my, volX - 10, volY - 12, volW + 20, volH + 24)) {
            draggingVolume = true;
            volume = clamp((mx - volX) / volW, 0f, 1f);
            MusicLoader.setVolume(volume);
            controlsLayerDirty = true;
            lastDragRender = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private boolean playlistContains(float mx, float my) {
        float s = pageScale();
        float w = Math.min(300f * s, width * 0.30f);
        float h = Math.min(260f * s, height * 0.48f);
        float x = width - w - 26f * s;
        float y = height - h - 88f * s;
        return inside(mx, my, x, y, w, h);
    }

    private int playlistRowAt(float mx, float my) {
        float s = pageScale();
        float h = Math.min(260f * s, height * 0.48f);
        float y = height - h - 88f * s;
        if (!playlistContains(mx, my)) return -1;
        return (int) ((my - y - 34f * s) / (26f * s));
    }

    private void nextTrack() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        trackIndex = shuffleMode && count > 1 ? (trackIndex + 1 + Math.max(1, (int)(System.nanoTime() % (count - 1)))) % count : (trackIndex + 1) % count;
        refreshTrackCache();
        playSelectedTrackFromStart();
        dirty = true;
    }

    private void previousTrack() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) return;
        trackIndex = (trackIndex - 1 + count) % count;
        refreshTrackCache();
        playSelectedTrackFromStart();
        dirty = true;
    }

    private void playSelectedTrackFromStart() {
        List<MusicLoader.MusicTrack> tracks = MusicLoader.getTracks();
        if (tracks.isEmpty()) return;
        clampTrackIndex();
        MusicLoader.play(tracks.get(trackIndex));
        resetPlaybackUiState();
    }

    private void playDisplayedTrackFrom(float seconds) {
        if (currentUiTrack == null) return;
        MusicLoader.playFrom(currentUiTrack, clamp(seconds, 0f, Math.max(0, currentUiTrack.duration())));
        resetPlaybackUiState();
    }

    private void finishProgressDrag() {
        if (currentUiTrack != null && progressDragSeconds >= 0f) playDisplayedTrackFrom(progressDragSeconds);
        progressDragSeconds = -1f;
        draggingProgress = false;
        controlsLayerDirty = true;
        lyricsLayerDirty = true;
    }

    private void resetPlaybackUiState() {
        lastRenderedSecond = -1;
        lastActiveLyric = -1;
        lyricSnapOnNextRender = true;
        controlsLayerDirty = true;
        lyricsLayerDirty = true;
    }

    private boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void rebuildCover(MusicLoader.MusicTrack track) {
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
        byte[] coverBytes = track.coverArt();
        if (coverBytes == null || coverBytes.length == 0) {
            derivePaletteFromText(track.title() + track.artist());
            rebuildFluidBackground(null, track);
            return;
        }
        try {
            coverImage = Image.makeFromEncoded(coverBytes);
            derivePaletteFromCover(coverBytes);
            rebuildFluidBackground(coverBytes, track);
        } catch (Exception e) {
            derivePaletteFromText(track.title() + track.artist());
            rebuildFluidBackground(null, track);
            SkijaTestClient.LOGGER.warn("[Musicpage] Failed to decode cover art for {}", track.title(), e);
        }
    }

    private void rebuildFluidBackground(byte[] coverBytes, MusicLoader.MusicTrack track) {
        String seed = track.title() + track.artist();
        if (gpuBackground != null) {
            gpuBackground.rebuild(coverBytes, seed);
            lastBgRender = 0L;
        }
    }

    private void derivePaletteFromCover(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return;
            long r = 0, g = 0, b = 0, count = 0;
            int stepX = Math.max(1, img.getWidth() / 28);
            int stepY = Math.max(1, img.getHeight() / 28);
            for (int y = 0; y < img.getHeight(); y += stepY) {
                for (int x = 0; x < img.getWidth(); x += stepX) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    if (a < 64) continue;
                    r += (argb >>> 16) & 0xFF;
                    g += (argb >>> 8) & 0xFF;
                    b += argb & 0xFF;
                    count++;
                }
            }
            if (count <= 0) return;
            int avg = rgb((int)(r / count), (int)(g / count), (int)(b / count));
            paletteA = saturate(lighten(avg, 0.14f), 1.22f);
            paletteB = saturate(rotate(avg), 1.34f);
            paletteC = saturate(darken(avg, 0.16f), 1.10f);
            paletteDark = darken(avg, 0.58f);
        } catch (Exception e) {
            SkijaTestClient.LOGGER.warn("[Musicpage] Failed to derive cover palette", e);
        }
    }

    private void derivePaletteFromText(String seed) {
        int h = Math.abs(seed == null ? 0 : seed.hashCode());
        int r = 110 + (h & 0x5F);
        int g = 48 + ((h >>> 7) & 0x4F);
        int b = 62 + ((h >>> 14) & 0x5F);
        int base = rgb(r, g, b);
        paletteA = saturate(lighten(base, 0.12f), 1.22f);
        paletteB = saturate(rotate(base), 1.30f);
        paletteC = darken(base, 0.16f);
        paletteDark = darken(base, 0.58f);
    }

    private void refreshTrackCache() {
        cachedTrack = null;
        clearLyricCache();
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
    }

    private List<LyricLine> parseLyrics(String rawLyrics, String title, String artist) {
        ArrayList<LyricLine> result = new ArrayList<>();
        if (rawLyrics != null && !rawLyrics.isBlank()) {
            String[] lines = rawLyrics.replace("\r", "").split("\n");
            int plainIndex = 0;
            for (String line : lines) {
                Matcher m = LRC_PATTERN.matcher(line);
                if (m.matches()) {
                    int min = parseInt(m.group(1));
                    int sec = parseInt(m.group(2));
                    int ms = parseMillis(m.group(3));
                    String text = m.group(4).isBlank() ? "♪" : m.group(4).trim();
                    result.add(new LyricLine(min * 60f + sec + ms / 1000f, text));
                } else if (!line.isBlank()) {
                    result.add(new LyricLine(plainIndex++ * 4.0f, line.trim()));
                }
            }
        }
        if (result.isEmpty()) {
            result.add(new LyricLine(0, title));
            result.add(new LyricLine(4, artist));
            result.add(new LyricLine(8, "把带歌词标签的 MP3 / FLAC 放到 lazychara/music"));
            result.add(new LyricLine(12, "之后接入真实播放进度即可同步歌词"));
        }
        result.sort(java.util.Comparator.comparingDouble(LyricLine::time));
        return result;
    }

    private int activeLyricIndex(float elapsed) {
        int active = 0;
        for (int i = 0; i < lyricLines.size(); i++) {
            if (elapsed + 0.05f >= lyricLines.get(i).time()) active = i;
            else break;
        }
        return active;
    }

    private float elapsedSeconds(MusicLoader.MusicTrack track, long now) {
        if (MusicLoader.getCurrentTrack() == track) {
            return MusicLoader.getCurrentSeconds();
        }
        return 0f;
    }

    private float displayedElapsedSeconds(MusicLoader.MusicTrack track, long now) {
        if (draggingProgress && track == currentUiTrack && progressDragSeconds >= 0f) {
            return clamp(progressDragSeconds, 0f, Math.max(0, track.duration()));
        }
        return elapsedSeconds(track, now);
    }

    private void blitLayers(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        float entryT = entryProgress(now);
        float returnT = returningToMain ? returnProgress(now) : 0f;
        float closeT = closingPage ? closeProgress(now) : 0f;
        var pose = g.pose();
        pose.pushMatrix();
        float entryE = easeOutCubic(entryT);
        float returnE = easeInOut(returnT);
        float closeE = easeInOut(closeT);
        float scale = (0.985f + 0.015f * entryE) * (1f - 0.070f * returnE) * (1f - 0.055f * closeE);
        float y = 18f * (1f - entryE) + 42f * returnE + 18f * closeE;
        pose.translate(width * 0.5f, height * 0.5f + y);
        pose.scale(scale, scale);
        pose.translate(-width * 0.5f, -height * 0.5f);
        blitBackgroundRenderer(g);
        blitRenderer(g, staticRenderer, 0f, 0f);
        blitRenderer(g, controlsRenderer, controlsLayerX, controlsLayerY);
        if (!blitLyricLines(g)) blitLyricsRenderer(g);
        pose.popMatrix();
        float entryA = 220f * (1f - easeInOut(entryT));
        float returnA = 225f * returnE;
        float closeA = 205f * closeE;
        int a = Math.max(0, Math.min(240, Math.round(Math.max(entryA, Math.max(returnA, closeA)))));
        if (a > 0) g.fill(0, 0, width, height, a << 24);
    }

    private float entryProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - entryStartedAt) / (float) ENTRY_ANIM_MS));
    }

    private float returnProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - exitStartedAt) / (float) RETURN_ANIM_MS));
    }

    private float closeProgress(long now) {
        return Math.max(0f, Math.min(1f, (now - exitStartedAt) / (float) CLOSE_ANIM_MS));
    }

    private float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private float easeInOut(float t) {
        t = clamp(t, 0f, 1f);
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2f) * 0.5f;
    }

    private void blitBackgroundRenderer(GuiGraphicsExtractor g) {
        if (gpuBackground != null && gpuBackground.draw(g, width, height, backgroundRenderTime, backgroundLowFreqPulse)) {
            g.fill(0, 0, width, height, 0x26000000);
            return;
        }
        if (bgRenderer == null) return;
        float inv = 1f / guiScale;
        float sx = (width * guiScale) / (float) bgRenderer.getWidth();
        float sy = (height * guiScale) / (float) bgRenderer.getHeight();
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(inv, inv);
        pose.scale(sx, sy);
        g.blit(RenderPipelines.GUI_TEXTURED, bgRenderer.textureId(), 0, 0, 0f, 0f, bgRenderer.getWidth(), bgRenderer.getHeight(), bgRenderer.getWidth(), bgRenderer.getHeight());
        pose.popMatrix();
    }

    private boolean blitLyricLines(GuiGraphicsExtractor g) {
        if (!useLineLyricTextures()) return false;
        SkijaTestScreen.ensureFontLoaded();
        Typeface tf = SkijaTestScreen.curTf;
        float s = pageScale();
        float activePreferred = Math.max(22f, 28f * s);
        float inactivePreferred = Math.max(17f, 24f * s);
        int active = lastActiveLyric >= 0 ? lastActiveLyric : activeLyricIndex(elapsedSeconds(currentUiTrack, System.currentTimeMillis()));
        if (ensureLyricCache(tf, layoutRightW, activePreferred, inactivePreferred, s)) {
            updateLyricLineAnimations(active, s, 0f, true);
        }
        if (lyricCache.size() != lyricLines.size() || lyricCache.isEmpty()) return false;
        float clipX = layoutRightX - 24f * s;
        float clipY = layoutLyricTop;
        float clipW = layoutRightW + 48f * s;
        float clipH = layoutLyricH;
        for (int i = 0; i < lyricCache.size(); i++) {
            CachedLyricLine line = lyricCache.get(i);
            boolean isActive = i == active;
            SkijaRenderer lineRenderer = isActive ? line.activeRenderer : line.inactiveRenderer;
            if (lineRenderer == null || !line.initialized) continue;
            float alpha = clamp(line.currentOpacity, 0f, 1f);
            if (alpha <= 0.01f) continue;
            float scale = clamp(line.currentScale, 0.86f, 1.08f);
            float texW = isActive ? line.activeW : line.inactiveW;
            float texH = isActive ? line.activeH : line.inactiveH;
            float pad = isActive ? line.activePad : line.inactivePad;
            float contentH = Math.max(1f, texH - pad * 2f);
            float rawX = layoutRightX - pad;
            float rawY = line.currentY - pad;
            float originX = layoutRightX;
            float originY = line.currentY + contentH * 0.5f;
            float drawX = originX + (rawX - originX) * scale;
            float drawY = originY + (rawY - originY) * scale;
            float drawW = texW * scale;
            float drawH = texH * scale;
            blitRendererClippedAlpha(g, lineRenderer, drawX, drawY, drawW, drawH, alpha, clipX, clipY, clipW, clipH);
        }
        return true;
    }

    private void blitRendererClippedAlpha(GuiGraphicsExtractor g, SkijaRenderer r, float dstX, float dstY, float dstW, float dstH, float alpha, float clipX, float clipY, float clipW, float clipH) {
        if (r == null || dstW <= 0.5f || dstH <= 0.5f || clipW <= 0.5f || clipH <= 0.5f) return;
        float left = Math.max(dstX, clipX);
        float top = Math.max(dstY, clipY);
        float right = Math.min(dstX + dstW, clipX + clipW);
        float bottom = Math.min(dstY + dstH, clipY + clipH);
        if (right <= left + 0.5f || bottom <= top + 0.5f) return;
        float u0 = (left - dstX) / dstW;
        float v0 = (top - dstY) / dstH;
        float u1 = (right - dstX) / dstW;
        float v1 = (bottom - dstY) / dstH;
        blitRendererAlpha(g, r, left, top, right - left, bottom - top, u0, v0, u1, v1, alpha);
    }

    private void blitRendererAlpha(GuiGraphicsExtractor g, SkijaRenderer r, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float alpha) {
        if (r == null || w <= 0.5f || h <= 0.5f) return;
        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(r.textureId());
        if (abstractTexture == null || abstractTexture.getTextureView() == null || abstractTexture.getSampler() == null) return;
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle scissor = g.scissorStack.peek();
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bw = Math.max(1, (int) Math.ceil(x + w) - bx);
        int bh = Math.max(1, (int) Math.ceil(y + h) - by);
        ScreenRectangle bounds = new ScreenRectangle(bx, by, bw, bh).transformMaxBounds(pose);
        TextureSetup textureSetup = TextureSetup.singleTexture(abstractTexture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        int a = Math.max(0, Math.min(255, Math.round(clamp(alpha, 0f, 1f) * 255f)));
        g.guiRenderState.addGuiElement(new TexturedQuadRenderState(RenderPipelines.GUI_TEXTURED, textureSetup, pose, x, y, w, h, u0, v0, u1, v1, (a << 24) | 0x00FFFFFF, scissor, bounds));
    }

    private void blitLyricsRenderer(GuiGraphicsExtractor g) {
        if (lyricsRenderer == null) return;
        float dstX = lyricsLayerX;
        float dstY = layoutLyricTop;
        float dstW = lyricsLayerW;
        float dstH = layoutLyricH;
        float srcX = 0f;
        float srcY = layoutLyricTop - lyricsLayerY - lyricLayerBlitOffsetY;
        float logicalH = lyricsRenderer.getHeight() / (float) guiScale;
        if (srcY < 0f) {
            float cut = -srcY;
            srcY = 0f;
            dstY += cut;
            dstH -= cut;
        }
        if (srcY + dstH > logicalH) dstH = logicalH - srcY;
        if (dstH <= 0.5f || dstW <= 0.5f) return;
        blitRendererRegion(g, lyricsRenderer, dstX, dstY, dstW, dstH, srcX, srcY, dstW, dstH);
    }

    private void blitRenderer(GuiGraphicsExtractor g, SkijaRenderer r, float x, float y) {
        if (r == null) return;
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(inv, inv);
        int px = Math.round(x * guiScale);
        int py = Math.round(y * guiScale);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), px, py, 0f, 0f, r.getWidth(), r.getHeight(), r.getWidth(), r.getHeight());
        pose.popMatrix();
    }

    private void blitRendererRegion(GuiGraphicsExtractor g, SkijaRenderer r, float dstX, float dstY, float dstW, float dstH, float srcX, float srcY, float srcW, float srcH) {
        if (r == null) return;
        int srcPxX = Math.max(0, Math.round(srcX * guiScale));
        int srcPxY = Math.max(0, Math.round(srcY * guiScale));
        int srcPxW = Math.max(1, Math.min(r.getWidth() - srcPxX, Math.round(srcW * guiScale)));
        int srcPxH = Math.max(1, Math.min(r.getHeight() - srcPxY, Math.round(srcH * guiScale)));
        int dstPxW = Math.max(1, Math.round(dstW * guiScale));
        int dstPxH = Math.max(1, Math.round(dstH * guiScale));
        float inv = 1f / guiScale;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(dstX, dstY);
        pose.scale(inv, inv);
        g.blit(RenderPipelines.GUI_TEXTURED, r.textureId(), 0, 0, (float) srcPxX, (float) srcPxY, dstPxW, dstPxH, srcPxW, srcPxH, r.getWidth(), r.getHeight());
        pose.popMatrix();
    }



    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (isExiting()) return true;
        if (sy > 0) {
            startReturnTransition();
            return true;
        }
        if (sy < 0 && !MusicLoader.getTracks().isEmpty()) {
            nextTrack();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (isExiting()) return true;
        if (e.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            startCloseTransition();
            return true;
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
            startReturnTransition();
            return true;
        }
        if (e.key() == GLFW.GLFW_KEY_R) {
            MusicLoader.rescan();
            clampTrackIndex();
            refreshTrackCache();
            dirty = true;
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        startCloseTransition();
    }

    private boolean isExiting() {
        return returningToMain || closingPage;
    }

    private void startReturnTransition() {
        if (isExiting()) return;
        returningToMain = true;
        exitStartedAt = System.currentTimeMillis();
        draggingVolume = false;
        draggingProgress = false;
        playlistOpen = false;
        dirty = true;
    }

    private void startCloseTransition() {
        if (isExiting()) return;
        closingPage = true;
        exitStartedAt = System.currentTimeMillis();
        draggingVolume = false;
        draggingProgress = false;
        playlistOpen = false;
        dirty = true;
    }

    @Override
    public void removed() {
        super.removed();
        closeRenderer();
    }

    private void closeRenderer() {
        if (gpuBackground != null) {
            gpuBackground.close();
            gpuBackground = null;
        }
        if (bgRenderer != null) {
            bgRenderer.close();
            bgRenderer = null;
        }
        if (staticRenderer != null) {
            staticRenderer.close();
            staticRenderer = null;
        }
        if (controlsRenderer != null) {
            controlsRenderer.close();
            controlsRenderer = null;
        }
        if (lyricsRenderer != null) {
            lyricsRenderer.close();
            lyricsRenderer = null;
        }
        renderer = null;
        if (coverImage != null) {
            coverImage.close();
            coverImage = null;
        }
        clearLyricCache();
        clearIconPathCache();
    }

    private void clearIconPathCache() {
        for (Path path : iconPathCache.values()) {
            if (path != null) path.close();
        }
        iconPathCache.clear();
    }

    private void clampTrackIndex() {
        int count = MusicLoader.getTracks().size();
        if (count <= 0) trackIndex = 0;
        else if (trackIndex < 0 || trackIndex >= count) trackIndex = 0;
    }

    private int[] computeBackgroundTextureSize() {
        int targetW = Math.max(1, Math.round(width * guiScale * BG_RENDER_SCALE));
        int targetH = Math.max(1, Math.round(height * guiScale * BG_RENDER_SCALE));
        float capScale = Math.min(1f, Math.min(BG_RENDER_MAX_W / (float) targetW, BG_RENDER_MAX_H / (float) targetH));
        targetW = Math.round(targetW * capScale);
        targetH = Math.round(targetH * capScale);
        targetW = Math.max(BG_RENDER_MIN_W, targetW);
        targetH = Math.max(BG_RENDER_MIN_H, targetH);
        return new int[]{targetW, targetH};
    }

    private float pageScale() {
        return clamp(Math.min(width / 620f, height / 300f), 0.62f, 1.35f);
    }

    private float fitTextSize(String text, Typeface tf, float preferredSize, float minSize, float maxWidth) {
        if (text == null || text.isEmpty()) return preferredSize;
        float size = preferredSize;
        while (size > minSize && renderer.measureText(text, tf, size) > maxWidth) {
            size -= 1.4f;
        }
        return Math.max(minSize, size);
    }

    private String trimText(String text, Typeface tf, float size, float maxWidth) {
        if (text == null) return "";
        if (renderer.measureText(text, tf, size) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && renderer.measureText(text.substring(0, end) + ellipsis, tf, size) > maxWidth) end--;
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return 0; }
    }

    private int parseMillis(String value) {
        if (value == null || value.isEmpty()) return 0;
        int raw = parseInt(value);
        if (value.length() == 1) return raw * 100;
        if (value.length() == 2) return raw * 10;
        return raw;
    }

    private String formatTime(int sec) {
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    private float sin(float v) { return (float) Math.sin(v); }
    private float cos(float v) { return (float) Math.cos(v); }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private float alphaOf(int color) { return ((color >>> 24) & 0xFF) / 255f; }

    private int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    private int clamp8(int v) { return Math.max(0, Math.min(255, v)); }

    private int lighten(int color, float amount) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb((int)(r + (255 - r) * amount), (int)(g + (255 - g) * amount), (int)(b + (255 - b) * amount));
    }

    private int darken(int color, float amount) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb((int)(r * (1f - amount)), (int)(g * (1f - amount)), (int)(b * (1f - amount)));
    }

    private int rotate(int color) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return rgb(Math.max(r, b), Math.max(g - 8, 0), Math.max(r - 20, 0));
    }

    private int saturate(int color, float factor) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        float gray = (r + g + b) / 3f;
        return rgb((int)(gray + (r - gray) * factor), (int)(gray + (g - gray) * factor), (int)(gray + (b - gray) * factor));
    }

    private record LyricLine(float time, String text) {}

    private record TextSegment(String text, float width, boolean isSpace) {}

    private record SpringParams(float stiffness, float damping) {}

    private record LyricPresentation(float opacity, float scale) {}

    private record TexturedQuadRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                                           float x, float y, float w, float h,
                                           float u0, float v0, float u1, float v1,
                                           int color, ScreenRectangle scissorArea, ScreenRectangle bounds)
            implements net.minecraft.client.renderer.state.gui.GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            vertexConsumer.addVertexWith2DPose(pose, x, y).setUv(u0, v0).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x, y + h).setUv(u0, v1).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x + w, y + h).setUv(u1, v1).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x + w, y).setUv(u1, v0).setColor(color);
        }
    }

    private static final class TextImage {
        final Image image;
        final SkijaRenderer renderer;
        final float w;
        final float h;
        final float pad;

        TextImage(Image image, SkijaRenderer renderer, float w, float h, float pad) {
            this.image = image;
            this.renderer = renderer;
            this.w = w;
            this.h = h;
            this.pad = pad;
        }
    }

    private static final class CachedLyricLine implements AutoCloseable {
        final Image activeImage;
        final SkijaRenderer activeRenderer;
        final float activeW;
        final float activeH;
        final float activePad;
        final Image inactiveImage;
        final SkijaRenderer inactiveRenderer;
        final float inactiveW;
        final float inactiveH;
        final float inactivePad;
        float currentY;
        float velocityY;
        float currentScale = 1f;
        float velocityScale;
        float currentOpacity = 1f;
        float velocityOpacity;
        boolean initialized;

        CachedLyricLine(TextImage active, TextImage inactive) {
            this.activeImage = active.image;
            this.activeRenderer = active.renderer;
            this.activeW = active.w;
            this.activeH = active.h;
            this.activePad = active.pad;
            this.inactiveImage = inactive.image;
            this.inactiveRenderer = inactive.renderer;
            this.inactiveW = inactive.w;
            this.inactiveH = inactive.h;
            this.inactivePad = inactive.pad;
        }

        @Override
        public void close() {
            if (activeImage != null) activeImage.close();
            if (inactiveImage != null) inactiveImage.close();
            if (activeRenderer != null) activeRenderer.close();
            if (inactiveRenderer != null) inactiveRenderer.close();
        }
    }
}
