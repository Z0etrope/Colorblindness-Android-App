   /**
     * Default screen gamma on Windows is 2.2.
     */
    private static final double GAMMA = 2.2;
    private static final double GAMMA_INV = 1. / GAMMA;

    /**
     * A lookup table for the conversion from gamma-corrected sRGB values
     * [0..255] to linear RGB values [0..32767].
     */
    private static final short[] SRGB_TO_LINRGB;

    static {
        // initialize SRGB_TO_LINRGB
        SRGB_TO_LINRGB = new short[256];
        for (int i = 0; i < 256; i++) {
            // compute linear rgb between 0 and 1
            final double lin = (0.992052 * Math.pow(i / 255., GAMMA) + 0.003974);

            // scale linear rgb to 0..32767
            SRGB_TO_LINRGB[i] = (short) (lin * 32767.);
        }
    }
    
    ImageView imageView = findViewById(R.id.image_for_filter);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.pic1, opts); //

        Bitmap new_bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.pic1, opts);

        int prevIn = 0;
        int prevOut = 0;

        //(9591, 23173, -730);
        int k1 = 9591;
        int k2 = 23173;
        int k3 = -730;

        for (int i = 0; i < bitmap.getWidth(); i++)
        {
            for (int j = 0; j < bitmap.getHeight(); j++)
            {
                //Color pixel = bitmap.getColor(i, j);
                final int rgb = bitmap.getPixel(i, j);
                final int r = (0xff0000 & rgb) >> 16;
                final int g = (0xff00 & rgb) >> 8;
                final int b = 0xff & rgb;
                // get linear rgb values in the range 0..2^15-1
                final int r_lin = SRGB_TO_LINRGB[r];
                final int g_lin = SRGB_TO_LINRGB[g];
                final int b_lin = SRGB_TO_LINRGB[b];

                int r_blind = (int) (k1 * r_lin + k2 * g_lin) >> 22;
                int b_blind = (int) (k3 * r_lin - k3 * g_lin + 32768 * b_lin) >> 22;

                if (r_blind < 0) {
                    r_blind = 0;
                } else if (r_blind > 255) {
                    r_blind = 255;
                }

                if (b_blind < 0) {
                    b_blind = 0;
                } else if (b_blind > 255) {
                    b_blind = 255;
                }

                // convert reduced linear rgb to gamma corrected rgb
                int red = LINRGB_TO_SRGB[r_blind];
                red = red >= 0 ? red : 256 + red; // from unsigned to signed
                int blue = LINRGB_TO_SRGB[b_blind];
                blue = blue >= 0 ? blue : 256 + blue; // from unsigned to signed

                final int out = 0xff000000 | red << 16 | red << 8 | blue;

                new_bitmap.setPixel(i, j, out);
                prevIn = rgb;
                prevOut = out;
            }
        }

        imageView.setImageBitmap(new_bitmap);