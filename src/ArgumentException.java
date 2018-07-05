/*
 * Name:		ArgumentException
 * Version:		0.1
 * Date:		2013-02-27
 * Author:		Volodymyr Kleban
 * 
 * Copyright (C) S.W.I.F.T. sc. 2013. All rights reserved.
 *
 *
 * This software and its associated documentation contain
 * proprietary, confidential and trade secret information of
 * S.W.I.F.T. sc. and except as provided by written agreement
 * with S.W.I.F.T. sc.
 * a) no part may be disclosed, distributed, reproduced,
 *    transmitted, transcribed, stored in a retrieval system,
 *    adapted or translated in any form or by any means
 *    electronic, mechanical, magnetic, optical, chemical,
 *    manual or otherwise, and
 * b) the recipient is not entitled to discover through reverse
 *    engineering or reverse compiling or other such techniques
 *    or processes the trade secrets contained in the software
 *    code or to use any such trade secrets contained therein or
 *    in the documentation.
 */

public class ArgumentException extends Exception {

	/**
	 * Exception enforces it
	 */
	private static final long serialVersionUID = -2743745010947259737L;

	public ArgumentException(String message) {
		super(message);
	}
}
