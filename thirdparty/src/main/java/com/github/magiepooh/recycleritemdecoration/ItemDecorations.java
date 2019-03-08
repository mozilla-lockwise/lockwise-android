/**
 * Copyright (C) 2015 magiepooh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.magiepooh.recycleritemdecoration;

import android.content.Context;

/**
 * Created by magiepooh on 2015/08
 */
public class ItemDecorations {

    public static VerticalItemDecoration.Builder vertical(Context context) {
        return new VerticalItemDecoration.Builder(context);
    }
}