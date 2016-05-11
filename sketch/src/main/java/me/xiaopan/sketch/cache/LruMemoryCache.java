/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.sketch.cache;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;
import android.util.Log;

import me.xiaopan.sketch.drawable.RecycleDrawable;
import me.xiaopan.sketch.Sketch;
import me.xiaopan.sketch.util.LruCache;
import me.xiaopan.sketch.util.SketchUtils;

public class LruMemoryCache implements MemoryCache {
    private static final String NAME = "LruMemoryCache";
    private Context context;
    private LruCache<String, Drawable> drawableLruCache;

    public LruMemoryCache(Context context, int maxSize) {
        this.context = context;
        this.drawableLruCache = new DrawableLruCache(maxSize);
    }

    @Override
    public synchronized void put(String key, Drawable value) {
        if (!(value instanceof RecycleDrawable)) {
            throw new IllegalArgumentException("drawable must be implemented RecycleDrawableInterface");
        }
        int cacheSize = 0;
        if (Sketch.isDebugMode()) {
            cacheSize = drawableLruCache.size();
        }
        drawableLruCache.put(key, value);
        if (Sketch.isDebugMode()) {
            Log.i(Sketch.TAG, SketchUtils.concat(NAME, " - ", "put", " - ", "beforeCacheSize=", Formatter.formatFileSize(context, cacheSize), " - ", ((RecycleDrawable) value).getInfo(), " - ", "afterCacheSize=", Formatter.formatFileSize(context, drawableLruCache.size())));
        }
    }

    @Override
    public synchronized Drawable get(String key) {
        return drawableLruCache.get(key);
    }

    @Override
    public synchronized Drawable remove(String key) {
        Drawable drawable = drawableLruCache.remove(key);
        if (Sketch.isDebugMode()) {
            Log.i(Sketch.TAG, SketchUtils.concat(NAME, " - ", "remove", " - ", "MemoryCacheSize: ", Formatter.formatFileSize(context, drawableLruCache.size())));
        }
        return drawable;
    }

    @Override
    public long getSize() {
        return drawableLruCache.size();
    }

    @Override
    public long getMaxSize() {
        return drawableLruCache.maxSize();
    }

    @Override
    public synchronized void clear() {
        if (Sketch.isDebugMode()) {
            Log.i(Sketch.TAG, SketchUtils.concat(NAME, " - ", "clear", " - ", "before clean MemoryCacheSize: ", Formatter.formatFileSize(context, drawableLruCache.size())));
        }
        drawableLruCache.evictAll();
    }

    @Override
    public void close() {
        clear();
    }

    @Override
    public String getIdentifier() {
        return appendIdentifier(new StringBuilder()).toString();
    }

    @Override
    public StringBuilder appendIdentifier(StringBuilder builder) {
        return builder.append(NAME)
                .append(". ")
                .append("maxSize").append("=").append(Formatter.formatFileSize(context, getMaxSize()));
    }

    private class DrawableLruCache extends LruCache<String, Drawable> {

        public DrawableLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        public Drawable put(String key, Drawable value) {
            ((RecycleDrawable) value).setIsCached(NAME + ":put", true);
            return super.put(key, value);
        }

        @Override
        public int sizeOf(String key, Drawable value) {
            int bitmapSize = ((RecycleDrawable) value).getByteCount();
            return bitmapSize == 0 ? 1 : bitmapSize;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Drawable oldValue, Drawable newValue) {
            ((RecycleDrawable) oldValue).setIsCached(NAME + ":entryRemoved", false);
        }
    }

    public static LruMemoryCache create(Context context){
        return new LruMemoryCache(context, (int) (Runtime.getRuntime().maxMemory() / 8));
    }

    public static LruMemoryCache createPlaceholder(Context context){
        return new LruMemoryCache(context, Math.min((int) (Runtime.getRuntime().maxMemory() / 16), 8 * 1024 * 1024));
    }
}