package com.jpiche.redditroulette.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

public final class GIFView extends View {

	private final Movie mMovie;
	private long movieStart;

	private final Paint paint = new Paint();

	public GIFView(Context context, final Movie movie) {
		super(context);
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mMovie = movie;

		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		//paint.setDither(true);
	}

	protected void onDraw(final Canvas canvas) {
		canvas.drawColor(Color.TRANSPARENT);
		super.onDraw(canvas);
		long now = SystemClock.uptimeMillis();

		final float scale = Math.min((float)getWidth() / mMovie.width(), (float)getHeight() / mMovie.height());

		canvas.scale(scale, scale);
		canvas.translate(((float)getWidth() / scale - (float)mMovie.width())/2f,
				((float)getHeight() / scale - (float)mMovie.height())/2f);


		if(movieStart == 0) movieStart = (int)now;

		mMovie.setTime((int)((now - movieStart) % mMovie.duration()));
		mMovie.draw(canvas, 0, 0, paint);

		this.invalidate();
	}
}