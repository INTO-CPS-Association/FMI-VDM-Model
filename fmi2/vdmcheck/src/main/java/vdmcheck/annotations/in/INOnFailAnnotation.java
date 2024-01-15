/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	MaestroCheck is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	MaestroCheck is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with MaestroCheck. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmcheck.annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;
import maestro.OnFailError;

import java.util.List;
import java.util.Vector;

public class INOnFailAnnotation extends annotations.in.INOnFailAnnotation {
    private static final long serialVersionUID = 1L;
    private static List<OnFailError> errorList = new Vector<>();

    public INOnFailAnnotation(TCIdentifierToken name, INExpressionList args, String format, INAnnotationList doclinks) {
        super(name, args, format, doclinks);
    }

    public INOnFailAnnotation(TCIdentifierToken name, INExpressionList args) {
        this(name, args, "", null);
    }

    @Override
    public void inAfter(INExpression exp, Value rv, Context ctxt) {
        try {
            if (!rv.boolValue(ctxt)) {
                int errno = 0;
                int offset = 1;
                if (this.args.get(0) instanceof INIntegerLiteralExpression) {
                    INIntegerLiteralExpression num = (INIntegerLiteralExpression) this.args.get(0);
                    errno = Long.valueOf(num.value.value).intValue();
                    offset = 2;
                }

                Object[] values = new Value[this.args.size() - offset];

                for (int p = offset; p < this.args.size(); ++p) {
                    values[p - offset] = ((INExpression) this.args.get(p)).eval(ctxt);
                }

                String location = "";
                String useformat = this.format;
                if (this.format.endsWith("$")) {
                    location = this.name.getLocation().toString();
                    useformat = this.format.substring(0, this.format.length() - 1);
                }
                errorList.add(new OnFailError(errno, String.format(useformat + location, values), doclinks));
            }
        } catch (ValueException var9) {
        }

    }

    public static void setErrorList(List<OnFailError> errors) {
        errorList = errors;
    }
}
