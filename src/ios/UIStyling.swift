/*
* Copyright (c) 2020 Elastos Foundation
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

import Foundation

class UIStyling {
    public static var popupMainTextColor = UIColor.black
    public static var popupInputHintTextColor = UIColor.black
    public static var popupMainBackgroundColor = UIColor.black
    public static var popupSecondaryBackgroundColor = UIColor.black
    
    static func prepare(useDarkMode: Bool) {
        if useDarkMode {
            // DARK MODE
            popupMainTextColor = UIColor.init(hex: "#fdfeff")!
            popupInputHintTextColor = UIColor.init(hex: "#fdfeff")!
            popupMainBackgroundColor = UIColor.init(hex: "#2e2f4e")!
            popupSecondaryBackgroundColor = UIColor.init(hex: "#1c1d34")!
        }
        else {
            // LIGHT MODE
            popupMainTextColor = UIColor.init(hex: "#161740")!
            popupInputHintTextColor = UIColor.init(hex: "#161740")!
            popupMainBackgroundColor = UIColor.init(hex: "#F0F0F0")!
            popupSecondaryBackgroundColor = UIColor.init(hex: "#FFFFFF")!
        }
    }
}
