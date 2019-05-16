/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.os.Build
import android.os.Parcel
import android.os.ParcelFormatException
import android.os.Parcelable
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi

open class ParsedStructureData<Id>(
    val username: Id? = null,
    val password: Id? = null,
    val webDomain: String? = null,
    val packageName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedStructureData<*>) return false

        if (username != other.username) return false
        if (password != other.password) return false
        if (webDomain != other.webDomain) return false
        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username?.hashCode() ?: 0
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (webDomain?.hashCode() ?: 0)
        result = 31 * result + packageName.hashCode()
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class ParsedStructure(
    usernameId: AutofillId? = null,
    passwordId: AutofillId? = null,
    webDomain: String? = null,
    packageName: String
) : ParsedStructureData<AutofillId>(usernameId, passwordId, webDomain, packageName), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(AutofillId::class.java.classLoader),
        parcel.readParcelable(AutofillId::class.java.classLoader),
        parcel.readString(),
        parcel.readString() ?: throw ParcelFormatException("packageName")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(this.username, flags)
        parcel.writeParcelable(this.password, flags)
        parcel.writeString(this.webDomain)
        parcel.writeString(this.packageName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParsedStructure> {
        override fun createFromParcel(parcel: Parcel): ParsedStructure {
            return ParsedStructure(parcel)
        }

        override fun newArray(size: Int): Array<ParsedStructure?> {
            return arrayOfNulls(size)
        }
    }
}